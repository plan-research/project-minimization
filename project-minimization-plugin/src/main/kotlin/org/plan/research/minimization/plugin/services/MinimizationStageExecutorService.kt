package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDD
import org.plan.research.minimization.plugin.errors.MinimizationError
import org.plan.research.minimization.plugin.execution.SameExceptionPropertyTester
import org.plan.research.minimization.plugin.getDDAlgorithm
import org.plan.research.minimization.plugin.getExceptionComparator
import org.plan.research.minimization.plugin.getHierarchyCollectionStrategy
import org.plan.research.minimization.plugin.lenses.FunctionModificationLens
import org.plan.research.minimization.plugin.logging.statLogger
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import mu.KotlinLogging
import org.plan.research.minimization.plugin.model.*

@Service(Service.Level.PROJECT)
class MinimizationStageExecutorService(private val project: Project) : MinimizationStageExecutor {
    private val generalLogger = KotlinLogging.logger {}
    private val cloningService = project.service<ProjectCloningService>()

    private suspend fun makeLight(context: IJDDContext): LightIJDDContext = when (context) {
        is HeavyIJDDContext -> {
            val projectDir = context.projectDir
            val result = LightIJDDContext(
                projectDir, context.originalProject,
                context.currentLevel, context.progressReporter,
            )

            ProjectManagerEx.getInstanceEx().forceCloseProjectAsync(context.project)

            result
        }

        is LightIJDDContext -> context
    }

    private suspend fun Raise<MinimizationError>.makeHeavy(context: IJDDContext): HeavyIJDDContext = when (context) {
        is HeavyIJDDContext -> context
        is LightIJDDContext -> {
            val openedProject = cloningService.openProject(context.projectDir.toNioPath(), true)
                ?: raise(MinimizationError.CloningFailed)
            HeavyIJDDContext(openedProject, context.originalProject, context.currentLevel, context.progressReporter)
        }
    }

    override suspend fun executeFileLevelStage(context: IJDDContext, fileLevelStage: FileLevelStage) = either {
        generalLogger.info { "Start File level stage" }
        statLogger.info {
            "File level stage settings, " +
                "Hierarchy strategy: ${fileLevelStage.hierarchyCollectionStrategy::class.simpleName}, " +
                "DDAlgorithm: ${fileLevelStage.ddAlgorithm::class.simpleName}"
        }

        val heavyContext = makeHeavy(context)

        val baseAlgorithm = fileLevelStage.ddAlgorithm.getDDAlgorithm()
        val hierarchicalDD = HierarchicalDD(baseAlgorithm)

        generalLogger.info { "Initialise file hierarchy" }
        val hierarchy = fileLevelStage
            .hierarchyCollectionStrategy
            .getHierarchyCollectionStrategy()
            .produce(heavyContext)
            .getOrElse { raise(MinimizationError.HierarchyFailed(it)) }

        generalLogger.info { "Minimize" }
        heavyContext.withProgress {
            hierarchicalDD.minimize(it, hierarchy)
        }
    }.logResult("File")

    override suspend fun executeFunctionLevelStage(
        context: IJDDContext,
        functionLevelStage: FunctionLevelStage,
    ) = either {
        generalLogger.info { "Start Function level stage" }
        statLogger.info {
            "Function level stage settings. DDAlgorithm: ${functionLevelStage.ddAlgorithm::class.simpleName}"
        }

        val lightContext = makeLight(context)

        val ddAlgorithm = functionLevelStage.ddAlgorithm.getDDAlgorithm()
        val exceptionComparator = project
            .service<MinimizationPluginSettings>()
            .state
            .exceptionComparingStrategy
            .getExceptionComparator()
        val lens = FunctionModificationLens()
        val firstLevel = project
            .service<MinimizationPsiManager>()
            .findAllPsiWithBodyItems()
        val propertyChecker = SameExceptionPropertyTester.create<PsiWithBodyDDItem>(
            this@MinimizationStageExecutorService.project.service<BuildExceptionProviderService>(),
            exceptionComparator,
            lens,
            lightContext,
        ).getOrElse {
            generalLogger.error { "Property checker creation failed. Aborted" }
            raise(MinimizationError.PropertyCheckerFailed)
        }

        firstLevel.logPsiElements()
        context
            .copy(currentLevel = firstLevel)
            .withProgress {
                ddAlgorithm.minimize(
                    it,
                    firstLevel,
                    propertyChecker,
                ).context
            }
    }.logResult("Function")

    private suspend fun List<PsiWithBodyDDItem>.logPsiElements() {
        if (!generalLogger.isTraceEnabled) {
            return
        }
        val psiManagingService = project.service<MinimizationPsiManager>()
        val text = mapNotNull {
            psiManagingService.getPsiElementFromItem(it)?.let { readAction { it.text } }
        }
        generalLogger.trace {
            "Starting DD Algorithm with following elements:\n" +
                text.joinToString("\n") { "\t- $it" }
        }
    }

    private fun <A, B> Either<A, B>.logResult(stageName: String) = onRight {
        generalLogger.info { "End $stageName level stage" }
        statLogger.info { "$stageName level stage result: success" }
    }.onLeft { error ->
        generalLogger.info { "End $stageName level stage" }
        statLogger.info { "$stageName level stage result: $error" }
        generalLogger.error { "$stageName level stage failed with error: $error" }
    }
}
