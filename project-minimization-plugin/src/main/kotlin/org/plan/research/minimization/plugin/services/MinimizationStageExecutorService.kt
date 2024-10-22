package org.plan.research.minimization.plugin.services

import arrow.core.getOrElse
import arrow.core.raise.either
import com.intellij.openapi.components.Service
import mu.KotlinLogging
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDD
import org.plan.research.minimization.plugin.errors.MinimizationError
import org.plan.research.minimization.plugin.getDDAlgorithm
import org.plan.research.minimization.plugin.getHierarchyCollectionStrategy
import org.plan.research.minimization.plugin.logging.statLogger
import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.MinimizationStageExecutor

@Service(Service.Level.PROJECT)
class MinimizationStageExecutorService : MinimizationStageExecutor {

    private val generalLogger = KotlinLogging.logger {}

    override suspend fun executeFileLevelStage(context: IJDDContext, fileLevelStage: FileLevelStage) = either {
        generalLogger.info { "Start File level stage" }
        statLogger.info { "File level stage settings, " +
                "Hierarchy strategy: ${fileLevelStage.hierarchyCollectionStrategy::class.simpleName}, " +
                "DDAlgorithm: ${fileLevelStage.ddAlgorithm::class.simpleName}" }

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
}
