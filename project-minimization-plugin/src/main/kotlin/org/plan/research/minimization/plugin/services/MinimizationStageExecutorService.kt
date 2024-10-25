package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDD
import org.plan.research.minimization.plugin.errors.MinimizationError
import org.plan.research.minimization.plugin.getDDAlgorithm
import org.plan.research.minimization.plugin.getHierarchyCollectionStrategy
import org.plan.research.minimization.plugin.logging.statLogger
import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.FunctionLevelStage
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.MinimizationStageExecutor

import arrow.core.getOrElse
import arrow.core.raise.either
import com.intellij.openapi.components.Service
import mu.KotlinLogging
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.plan.research.minimization.plugin.execution.SameExceptionPropertyTester
import org.plan.research.minimization.plugin.getExceptionComparator
import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem
import org.plan.research.minimization.plugin.psi.FunctionModificationLens
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings

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
    }.onRight {
        generalLogger.info { "End File level stage" }
        statLogger.info { "File level stage result: success" }
    }.onLeft { error ->
        generalLogger.info { "End File level stage" }
        statLogger.info { "File level stage result: $error" }
        generalLogger.error { "File level stage failed with error: $error" }
    }

    override suspend fun executeFunctionLevelStage(
        context: IJDDContext,
        functionLevelStage: FunctionLevelStage,
    ) = either {
        val ddAlgorithm = functionLevelStage.ddAlgorithm.getDDAlgorithm()
        val buildExceptionProvider = project.service<BuildExceptionProviderService>()
        val exceptionComparator = project
            .service<MinimizationPluginSettings>()
            .state
            .exceptionComparingStrategy
            .getExceptionComparator()
        val lens = FunctionModificationLens()
        val propertyChecker = SameExceptionPropertyTester.create<PsiWithBodyDDItem>(
            buildExceptionProvider,
            exceptionComparator,
            lens,
            context
        ).getOrElse { raise(MinimizationError.PropertyCheckerFailed) }
        context.withProgress {
            ddAlgorithm.minimize(
                it,
                project.service<PsiWithBodiesCollectorService>().getElementsWithBody(),
                propertyChecker
            ).context
        }
    }
}
