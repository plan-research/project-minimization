package org.plan.research.minimization.plugin.settings

import arrow.core.Either
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.core.algorithm.dd.impl.DDMin
import org.plan.research.minimization.core.algorithm.dd.impl.ProbabilisticDD
import org.plan.research.minimization.plugin.errors.CompilationPropertyCheckerError
import org.plan.research.minimization.plugin.hierarchy.FileTreeHierarchyGenerator
import org.plan.research.minimization.plugin.model.*

class MinimizationPluginState : BaseState() {
    var currentCompilationStrategy by enum<CompilationStrategy>(CompilationStrategy.GRADLE_IDEA)
    var currentHierarchyCollectionStrategy by enum<HierarchyCollectionStrategy>(HierarchyCollectionStrategy.FILE_TREE)
    var currentDDAlgorithm by enum<DDStrategy>(DDStrategy.DD_MIN)
    var temporaryProjectLocation by string("minimization-project-snapshots")
    fun getCompilationStrategy(): CompilationPropertyChecker = when (currentCompilationStrategy) {
        CompilationStrategy.GRADLE_IDEA -> TODO()
        CompilationStrategy.DUMB -> DUMB_COMPILER
    }

    fun getHierarchyCollectionStrategy(): ProjectHierarchyProducer =
        when (currentHierarchyCollectionStrategy) {
            HierarchyCollectionStrategy.FILE_TREE -> FileTreeHierarchyGenerator()
        }

    fun getDDAlgorithm(): DDAlgorithm = when (currentDDAlgorithm) {
        DDStrategy.DD_MIN -> DDMin()
        DDStrategy.PROBABILISTIC_DD -> ProbabilisticDD()
    }

    companion object {
        private val DUMB_COMPILER = object : CompilationPropertyChecker {
            override suspend fun checkCompilation(project: Project): Either<CompilationPropertyCheckerError, Throwable> =
                Either.Right(throwable)

            private val throwable = Throwable()
        }
    }
}