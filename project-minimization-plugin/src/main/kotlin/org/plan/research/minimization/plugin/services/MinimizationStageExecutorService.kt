package org.plan.research.minimization.plugin.services

import arrow.core.getOrElse
import arrow.core.raise.either
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDD
import org.plan.research.minimization.plugin.errors.MinimizationError
import org.plan.research.minimization.plugin.getDDAlgorithm
import org.plan.research.minimization.plugin.getHierarchyCollectionStrategy
import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.MinimizationStageExecutor

@Service(Service.Level.PROJECT)
class MinimizationStageExecutorService : MinimizationStageExecutor {
    override suspend fun executeFileLevelStage(project: Project, fileLevelStage: FileLevelStage) = either {
        val baseAlgorithm = fileLevelStage.ddAlgorithm.getDDAlgorithm()
        val hierarchicalDD = HierarchicalDD(baseAlgorithm)
        val hierarchy = fileLevelStage
            .hierarchyCollectionStrategy
            .getHierarchyCollectionStrategy()
            .produce(project)
            .getOrElse { raise(MinimizationError.HierarchyFailed(it)) }
        hierarchicalDD.minimize(hierarchy)
        TODO("retrieve somehow minimized project, probably change core (100%)")
    }
}


