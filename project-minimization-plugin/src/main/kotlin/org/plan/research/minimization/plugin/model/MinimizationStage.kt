package org.plan.research.minimization.plugin.model

import arrow.core.Either
import com.intellij.util.xmlb.annotations.Tag
import org.plan.research.minimization.plugin.errors.MinimizationError
import org.plan.research.minimization.plugin.model.state.DDStrategy
import org.plan.research.minimization.plugin.model.state.HierarchyCollectionStrategy

interface MinimizationStageExecutor {
    suspend fun executeFileLevelStage(
        context: IJDDContext,
        fileLevelStage: FileLevelStage
    ): Either<MinimizationError, IJDDContext>
}

sealed interface MinimizationStage {
    suspend fun apply(
        context: IJDDContext,
        executor: MinimizationStageExecutor
    ): Either<MinimizationError, IJDDContext>
}

@Tag("fileLevelStage")
data class FileLevelStage(
    var hierarchyCollectionStrategy: HierarchyCollectionStrategy,
    var ddAlgorithm: DDStrategy,
) : MinimizationStage {
    override suspend fun apply(context: IJDDContext, executor: MinimizationStageExecutor) =
        executor.executeFileLevelStage(context, this)
}
