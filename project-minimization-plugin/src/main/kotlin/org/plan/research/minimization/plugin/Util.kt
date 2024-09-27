package org.plan.research.minimization.plugin

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.core.algorithm.dd.impl.DDMin
import org.plan.research.minimization.core.algorithm.dd.impl.ProbabilisticDD
import org.plan.research.minimization.plugin.execution.DumbCompiler
import org.plan.research.minimization.plugin.hierarchy.FileTreeHierarchyGenerator
import org.plan.research.minimization.plugin.model.*


fun HierarchyCollectionStrategy.getHierarchyCollectionStrategy(): ProjectHierarchyProducer =
    when (this) {
        HierarchyCollectionStrategy.FILE_TREE -> FileTreeHierarchyGenerator()
    }

fun DDStrategy.getDDAlgorithm(): DDAlgorithm =
    when (this) {
        DDStrategy.DD_MIN -> DDMin()
        DDStrategy.PROBABILISTIC_DD -> ProbabilisticDD()
    }

fun CompilationStrategy.getCompilationStrategy(): CompilationPropertyChecker =
    when (this) {
        CompilationStrategy.GRADLE_IDEA -> TODO()
        CompilationStrategy.DUMB -> DumbCompiler
    }
