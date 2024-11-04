package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDD
import org.plan.research.minimization.plugin.errors.MinimizationError
import org.plan.research.minimization.plugin.execution.SameExceptionPropertyTester
import org.plan.research.minimization.plugin.getDDAlgorithm
import org.plan.research.minimization.plugin.getExceptionComparator
import org.plan.research.minimization.plugin.getHierarchyCollectionStrategy
import org.plan.research.minimization.plugin.hierarchy.DeletablePsiElementHierarchyGenerator
import org.plan.research.minimization.plugin.lenses.FunctionModificationLens
import org.plan.research.minimization.plugin.logging.statLogger
import org.plan.research.minimization.plugin.model.*
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import mu.KotlinLogging

@Service(Service.Level.PROJECT)
class MinimizationStageExecutorService(private val project: Project) : MinimizationStageExecutor {
    private val generalLogger = KotlinLogging.logger {}
    override suspend fun executeFileLevelStage(context: IJDDContext, fileLevelStage: FileLevelStage) = either {
        generalLogger.info { "Start File level stage" }
        statLogger.info {
            "File level stage settings, " +
                "Hierarchy strategy: ${fileLevelStage.hierarchyCollectionStrategy::class.simpleName}, " +
                "DDAlgorithm: ${fileLevelStage.ddAlgorithm::class.simpleName}"
        }

        val baseAlgorithm = fileLevelStage.ddAlgorithm.getDDAlgorithm()
        val hierarchicalDD = HierarchicalDD(baseAlgorithm)

        generalLogger.info { "Initialise file hierarchy" }
        val hierarchy = fileLevelStage
            .hierarchyCollectionStrategy
            .getHierarchyCollectionStrategy()
            .produce(context)
            .getOrElse { raise(MinimizationError.HierarchyFailed(it)) }

        generalLogger.info { "Minimize" }
        context.withProgress {
            hierarchicalDD.minimize(it, hierarchy)
        }
    }.logResult("File")

    override suspend fun executeFunctionLevelStage(
        context: IJDDContext,
        functionLevelBodyReplacementStage: FunctionLevelBodyReplacementStage,
    ) = either {
        generalLogger.info { "Start Function level stage" }
        statLogger.info {
            "Function level stage settings. DDAlgorithm: ${functionLevelBodyReplacementStage.ddAlgorithm::class.simpleName}"
        }
        val ddAlgorithm = functionLevelBodyReplacementStage.ddAlgorithm.getDDAlgorithm()
        val exceptionComparator = project
            .service<MinimizationPluginSettings>()
            .state
            .exceptionComparingStrategy
            .getExceptionComparator()
        val lens = FunctionModificationLens()
        val firstLevel = project
            .service<MinimizationPsiManager>()
            .findAllPsiWithBodyItems()
        val propertyChecker = SameExceptionPropertyTester.create<PsiDDItem>(
            this@MinimizationStageExecutorService.project.service<BuildExceptionProviderService>(),
            exceptionComparator,
            lens,
            context,
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
    }.logResult("Function Body Replacement")

    private suspend fun List<PsiDDItem>.logPsiElements() {
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

    override suspend fun executeFunctionDeletingStage(
        context: IJDDContext,
        functionDeletingStage: FunctionDeletingStage,
    ) = either {
        generalLogger.info { "Start Function deleting stage" }
        statLogger.info {
            "Function deleting stage settings, " +
                "DDAlgorithm: ${functionDeletingStage.ddAlgorithm::class.simpleName}"
        }
        val ddAlgorithm = functionDeletingStage.ddAlgorithm.getDDAlgorithm()
        val hierarchicalDD = HierarchicalDD(ddAlgorithm)
        val hierarchy = DeletablePsiElementHierarchyGenerator()
            .produce(context)
            .getOrElse { raise(MinimizationError.HierarchyFailed(it)) }

        context.withProgress {
            hierarchicalDD.minimize(it, hierarchy)
        }
    }.logResult("Function Deleting")

    private fun <A, B> Either<A, B>.logResult(stageName: String) = onRight {
        generalLogger.info { "End $stageName level stage" }
        statLogger.info { "$stageName level stage result: success" }
    }.onLeft { error ->
        generalLogger.info { "End $stageName level stage" }
        statLogger.info { "$stageName level stage result: $error" }
        generalLogger.error { "$stageName level stage failed with error: $error" }
    }
}
