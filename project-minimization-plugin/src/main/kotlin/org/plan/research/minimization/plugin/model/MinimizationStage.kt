package org.plan.research.minimization.plugin.model

import arrow.core.Either
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Tag
import org.plan.research.minimization.plugin.errors.MinimizationError

interface MinimizationStageExecutor {
    suspend fun executeFileLevelStage(
        project: Project,
        fileLevelStage: FileLevelStage
    ): Either<MinimizationError, Project>
}

sealed interface MinimizationStage {
    suspend fun apply(project: Project, executor: MinimizationStageExecutor): Either<MinimizationError, Project>
}

@Tag("fileLevelStage")
data class FileLevelStage(
    var hierarchyCollectionStrategy: HierarchyCollectionStrategy,
    var ddAlgorithm: DDStrategy,
) : MinimizationStage {
    override suspend fun apply(project: Project, executor: MinimizationStageExecutor) =
        executor.executeFileLevelStage(project, this)
}
