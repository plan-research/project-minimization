package org.plan.research.minimization.plugin.settings

import com.intellij.openapi.components.BaseState
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.core.algorithm.dd.impl.DDMin
import org.plan.research.minimization.core.algorithm.dd.impl.ProbabilisticDD
import org.plan.research.minimization.plugin.hierarchy.FileTreeHierarchyGenerator
import org.plan.research.minimization.plugin.model.*

class MinimizationPluginState : BaseState() {
    var currentCompilationStrategy by enum<CompilationStrategy>(CompilationStrategy.GRADLE_IDEA)
    var currentHierarchyCollectionStrategy by enum<HierarchyCollectionStrategy>(HierarchyCollectionStrategy.FILE_TREE)
    var currentDDAlgorithm by enum<DDStrategy>(DDStrategy.DD_MIN)
    var temporaryProjectLocation by string("minimization-project-snapshots")
    fun getCompilationStrategy(): CompilationPropertyChecker = when (currentCompilationStrategy) {
        CompilationStrategy.GRADLE_IDEA -> TODO()
    }

    fun getHierarchyCollectionStrategy(): ProjectHierarchyProducer =
        when (currentHierarchyCollectionStrategy) {
            HierarchyCollectionStrategy.FILE_TREE -> FileTreeHierarchyGenerator()
        }
    fun getDDAlgorithm(): DDAlgorithm = when (currentDDAlgorithm) {
        DDStrategy.DD_MIN -> DDMin()
        DDStrategy.PROBABILISTIC_DD -> ProbabilisticDD()
    }
}