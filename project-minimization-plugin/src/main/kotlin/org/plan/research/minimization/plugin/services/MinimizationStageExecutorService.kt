package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDD
import org.plan.research.minimization.plugin.errors.MinimizationError
import org.plan.research.minimization.plugin.getDDAlgorithm
import org.plan.research.minimization.plugin.getHierarchyCollectionStrategy
import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.MinimizationStageExecutor

import arrow.core.getOrElse
import arrow.core.raise.either
import com.intellij.openapi.components.Service

@Service(Service.Level.PROJECT)
class MinimizationStageExecutorService : MinimizationStageExecutor {
    override suspend fun executeFileLevelStage(context: IJDDContext, fileLevelStage: FileLevelStage) = either {
        val baseAlgorithm = fileLevelStage.ddAlgorithm.getDDAlgorithm()
        val hierarchicalDD = HierarchicalDD(baseAlgorithm)
        val hierarchy = fileLevelStage
            .hierarchyCollectionStrategy
            .getHierarchyCollectionStrategy()
            .produce(context)
            .getOrElse { raise(MinimizationError.HierarchyFailed(it)) }
        context.withProgress {
            hierarchicalDD.minimize(it, hierarchy)
        }
    }
}
