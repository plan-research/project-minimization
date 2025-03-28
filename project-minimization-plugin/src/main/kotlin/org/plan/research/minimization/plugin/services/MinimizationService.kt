package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.algorithm.MinimizationError
import org.plan.research.minimization.plugin.algorithm.stages.MinimizationStage
import org.plan.research.minimization.plugin.context.HeavyIJDDContext
import org.plan.research.minimization.plugin.context.IJDDContextBase
import org.plan.research.minimization.plugin.context.IJDDContextTransformer
import org.plan.research.minimization.plugin.context.LightIJDDContext
import org.plan.research.minimization.plugin.context.impl.DefaultProjectContext
import org.plan.research.minimization.plugin.logging.ExecutionDiscriminator
import org.plan.research.minimization.plugin.logging.statLogger
import org.plan.research.minimization.plugin.modification.psi.KDocRemover
import org.plan.research.minimization.plugin.modification.psi.PsiImportCleaner
import org.plan.research.minimization.plugin.util.getCurrentTimeString
import org.plan.research.minimization.plugin.util.getMinimizationStage

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.right
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

typealias MinimizationResult = Either<MinimizationError, HeavyIJDDContext<*>>

@Service(Service.Level.PROJECT)
class MinimizationService(private val project: Project, private val coroutineScope: CoroutineScope) {
    private val settings = project.service<MinimizationPluginSettings>()
    private val stages by settings
        .stateObservable
        .stages
        .observe { it }
    private val projectCloning = project.service<ProjectCloningService>()
    private val openingService = service<ProjectOpeningService>()
    private val logger = KotlinLogging.logger {}
    private val heavyTransformer = HeavyTransformer()

    fun minimizeProject(onComplete: suspend (HeavyIJDDContext<*>) -> Unit = { }) {
        coroutineScope.launch {
            settings.withFrozenState {
                withBackgroundProgress(project, "Minimizing project") {
                    minimizeProjectAsync().onRight { onComplete(it) }
                }
            }
        }
    }

    suspend fun minimizeProjectAsync(): MinimizationResult = withLoggingFolder {
        either {
            logger.info { "Start Project minimization" }
            statLogger.info { "Start Project minimization for project: ${project.name}" }

            var context: HeavyIJDDContext<*> = DefaultProjectContext(project)

            reportSequentialProgress(stages.size) { reporter ->
                context = cloneProject(context, reporter)
                removeKDocs(context)

                for (stageData in stages) {
                    val stage = stageData.getMinimizationStage()
                    logger.info { "Starting stage=${stage.stageName}. The starting snapshot is: ${context.projectDir.toNioPath()}" }
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
        context: HeavyIJDDContext<*>,
        stage: MinimizationStage,
        reporter: SequentialProgressReporter,
    ): HeavyIJDDContext<*> {
        val newContext = reporter.itemStep("Minimization step: ${stage.stageName}") {
            stage.executeStage(context).bind()
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
        context: HeavyIJDDContext<*>,
        reporter: SequentialProgressReporter,
    ): HeavyIJDDContext<*> {
        logger.info { "Clonning project..." }
        val result = reporter.indeterminateStep("Clonning project") {
            context.clone(projectCloning) ?: raise(MinimizationError.CloningFailed)
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
        oldContext: HeavyIJDDContext<*>,
        context: IJDDContextBase<*>,
    ): HeavyIJDDContext<*> {
        if (oldContext.projectDir == context.projectDir) {
            return oldContext
        }
        val newContext = context.transform(heavyTransformer).bind()

        // TODO: JBRes-2103 Resource Management
        ProjectManagerEx.getInstanceEx().forceCloseProjectAsync(oldContext.project)
        logger.info { "Made new heavy context: ${newContext.projectDir}" }
        return newContext
    }

    private suspend fun postProcess(context: HeavyIJDDContext<*>) {
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

    private suspend fun removeKDocs(context: HeavyIJDDContext<*>) {
        val kDocRemover = KDocRemover()
        try {
            kDocRemover.removeKDocs(context)
        } catch (e: Throwable) {
            logger.error(e) { "Error happened on removing the KDocs completely" }
        }
    }

    private inner class HeavyTransformer : IJDDContextTransformer<MinimizationResult> {
        override suspend fun transformLight(context: LightIJDDContext<*>) = either {
            val openedProject = openingService.openProject(context.projectDir.toNioPath())
                ?: raise(MinimizationError.OpeningFailed)
            DefaultProjectContext(openedProject, context.originalProject)
        }

        override suspend fun transformHeavy(context: HeavyIJDDContext<*>) =
            context.right()
    }
}
