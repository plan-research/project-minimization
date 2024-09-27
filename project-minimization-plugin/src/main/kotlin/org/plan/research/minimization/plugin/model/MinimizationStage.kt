package org.plan.research.minimization.plugin.model

import arrow.core.Either
import com.intellij.util.xmlb.annotations.Tag
import org.plan.research.minimization.plugin.errors.MinimizationError

interface MinimizationStageExecutor {
    suspend fun executeFileLevelStage(
        project: ProjectDDVersion,
        fileLevelStage: FileLevelStage
    ): Either<MinimizationError, ProjectDDVersion>
}

sealed interface MinimizationStage {
    suspend fun apply(
        project: ProjectDDVersion,
        executor: MinimizationStageExecutor
    ): Either<MinimizationError, ProjectDDVersion>
}

@Tag("fileLevelStage")
data class FileLevelStage(
    var hierarchyCollectionStrategy: HierarchyCollectionStrategy,
    var ddAlgorithm: DDStrategy,
) : MinimizationStage {
    override suspend fun apply(project: ProjectDDVersion, executor: MinimizationStageExecutor) =
        executor.executeFileLevelStage(project, this)
}
