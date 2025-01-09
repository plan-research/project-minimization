package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.errors.MinimizationError
import org.plan.research.minimization.plugin.logging.statLogger
import org.plan.research.minimization.plugin.getCurrentTimeString
import org.plan.research.minimization.plugin.logging.ExecutionDiscriminator
import org.plan.research.minimization.plugin.model.HeavyIJDDContext
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.LightIJDDContext
import org.plan.research.minimization.plugin.model.MinimizationStage
import org.plan.research.minimization.plugin.psi.PsiImportCleaner

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.project.waitForSmartMode
import com.intellij.openapi.vfs.findOrCreateDirectory
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.util.progress.reportSequentialProgress
import mu.KotlinLogging

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class MinimizationService(private val project: Project, private val coroutineScope: CoroutineScope) {
    private val settings = project.service<MinimizationPluginSettings>()
    private val stages by settings
        .stateObservable
        .stages
        .observe { it }
    private val executor = project.service<MinimizationStageExecutorService>()
    private val projectCloning = project.service<ProjectCloningService>()
    private val openingService = service<ProjectOpeningService>()
    private val logger = KotlinLogging.logger {}

    fun minimizeProject(onComplete: suspend (HeavyIJDDContext) -> Unit = { }) {
        coroutineScope.launch {
            settings.withFrozenState {
                withBackgroundProgress(project, "Minimizing project") {
                    minimizeProjectImpl().onRight { onComplete(it) }
                }
            }
        }
    }

    private suspend fun minimizeProjectImpl(): Either<MinimizationError, HeavyIJDDContext> = withLoggingFolder {
        either {
            logger.info { "Start Project minimization" }
            statLogger.info { "Start Project minimization for project: ${project.name}" }

            var context = HeavyIJDDContext(project)

            reportSequentialProgress(stages.size) { reporter ->
                context = cloneProject(context, reporter)

                for (stage in stages) {
                    logger.info { "Starting stage=${stage.name}. The starting snapshot is: ${context.projectDir.toNioPath()}" }
                    context = processStage(context, stage, reporter)
                }
            }

            context
        }.onRight {
            logger.info { "End Project minimization" }
            statLogger.info { "End Project minimization for project: ${project.name}" }
        }.onLeft { error ->
            logger.info { "End Project minimization" }
            statLogger.info { "End Project minimization for project: ${project.name}" }
            logger.error { "End minimizeProject with error: $error" }
        }
    }

    private suspend fun Raise<MinimizationError>.processStage(
        context: HeavyIJDDContext,
        stage: MinimizationStage,
        reporter: SequentialProgressReporter,
    ): HeavyIJDDContext {
        val newContext = reporter.itemStep("Minimization step: ${stage.name}") {
            stage.apply(context, executor).bind()
        }

        logger.info { "Opening and importing" }

        val result = reporter.indeterminateStep("Opening and importing") {
            makeHeavy(context, newContext)
        }

        logger.info { "Building indexes" }
        reporter.indeterminateStep("Building indexes") {
            result.project.waitForSmartMode()
        }

        logger.info { "Postprocessing" }
        reporter.indeterminateStep("Postprocessing") {
            if (context != result) {
                postProcess(result)
            }
        }

        return result
    }

    private suspend fun Raise<MinimizationError>.cloneProject(
        context: HeavyIJDDContext,
        reporter: SequentialProgressReporter,
    ): HeavyIJDDContext {
        logger.info { "Clonning project..." }
        val result = reporter.indeterminateStep("Clonning project") {
            projectCloning.clone(context) ?: raise(MinimizationError.CloningFailed)
        }
        logger.info { "Project clone end" }
        logger.info { "Wait for indexing" }
        reporter.indeterminateStep("Wait for indexing") {
            result.project.waitForSmartMode()
        }
        logger.info { "Postprocessing" }
        reporter.indeterminateStep("Postprocessing") {
            postProcess(result)
        }
        logger.info { "Postprocessing end" }
        return result
    }

    private suspend fun Raise<MinimizationError>.makeHeavy(
        oldContext: HeavyIJDDContext,
        context: IJDDContext,
    ): HeavyIJDDContext {
        if (oldContext.projectDir == context.projectDir) {
            return oldContext
        }
        val newContext = when (context) {
            is HeavyIJDDContext -> context
            is LightIJDDContext -> {
                val openedProject = openingService.openProject(context.projectDir.toNioPath())
                    ?: raise(MinimizationError.OpeningFailed)
                HeavyIJDDContext(
                    openedProject, context.originalProject,
                    context.currentLevel, context.progressReporter,
                    context.importRefCounter,
                )
            }
        }

        // TODO: JBRes-2103 Resource Management
        ProjectManagerEx.getInstanceEx().forceCloseProjectAsync(oldContext.project)
        logger.info { "Made new heavy context: ${newContext.projectDir}" }
        return newContext
    }

    private suspend fun postProcess(context: HeavyIJDDContext) {
        val importCleaner = PsiImportCleaner()
        try {
            importCleaner.cleanAllImports(context)
        } catch (e: Throwable) {
            logger.error(e) { "Error happened on the cleaning unused imports" }
        }
    }

    private suspend inline fun <T> withLoggingFolder(block: () -> T): T {
        val logsLocation = settings.stateObservable.logsLocation.get()
        val logsBaseDir = writeAction {
            project.guessProjectDir()!!.findOrCreateDirectory(logsLocation)
        }.toNioPath()

        val time = getCurrentTimeString()
        val executionId = "execution-$time"

        return ExecutionDiscriminator.withLoggingFolder(logsBaseDir, executionId, block)
    }
}
