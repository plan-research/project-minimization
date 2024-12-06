package org.plan.research.minimization.plugin.model

import org.plan.research.minimization.plugin.errors.MinimizationError
import org.plan.research.minimization.plugin.model.state.DDStrategy
import org.plan.research.minimization.plugin.model.state.HierarchyCollectionStrategy

import arrow.core.Either
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.Tag

/**
 * Interface representing an executor responsible for handling various stages of the minimization process.
 */
interface MinimizationStageExecutor {
    suspend fun executeFileLevelStage(
        context: HeavyIJDDContext,
        fileLevelStage: FileLevelStage,
    ): Either<MinimizationError, IJDDContext>

    suspend fun executeFunctionLevelStage(
        context: HeavyIJDDContext,
        functionLevelStage: FunctionLevelStage,
    ): Either<MinimizationError, IJDDContext>

    suspend fun executeDeclarationLevelStage(
        context: HeavyIJDDContext,
        declarationLevelStage: DeclarationLevelStage,
    ): Either<MinimizationError, IJDDContext>
}

/**
 * Represents a stage in the minimization process that can be applied to a context.
 *
 * A `MinimizationStage` defines a unit of work that can be executed during the minimization of a project.
 * Implementations of this interface should provide concrete steps to minimize the project's context.
 */
sealed interface MinimizationStage {
    val name: String

    suspend fun apply(
        context: HeavyIJDDContext,
        executor: MinimizationStageExecutor,
    ): Either<MinimizationError, IJDDContext>
}

/**
 * Represents a stage in the minimization process that operates at the file level.
 *
 * This class configures and applies a hierarchical delta debugging algorithm to minimize a project's file structure.
 *
 * @property hierarchyCollectionStrategy The strategy for collecting file hierarchy within the project.
 * @property ddAlgorithm The delta debugging algorithm to use for minimization.
 */
@Tag("fileLevelStage")
data class FileLevelStage(
    @Property val hierarchyCollectionStrategy: HierarchyCollectionStrategy = HierarchyCollectionStrategy.FILE_TREE,
    @Property val ddAlgorithm: DDStrategy = DDStrategy.PROBABILISTIC_DD,
) : MinimizationStage {
    override val name: String = "File-Level Minimization"

    override suspend fun apply(context: HeavyIJDDContext, executor: MinimizationStageExecutor) =
        executor.executeFileLevelStage(context, this)
}

@Tag("functionLevelStage")
data class FunctionLevelStage(
    @Property val ddAlgorithm: DDStrategy = DDStrategy.PROBABILISTIC_DD,
) : MinimizationStage {
    override val name: String = "Body Replacement Algorithm"

    override suspend fun apply(
        context: HeavyIJDDContext,
        executor: MinimizationStageExecutor,
    ): Either<MinimizationError, IJDDContext> = executor.executeFunctionLevelStage(context, this)
}

@Tag("declarationLevelStage")
data class DeclarationLevelStage(
    @Property val ddAlgorithm: DDStrategy = DDStrategy.PROBABILISTIC_DD,
) : MinimizationStage {
    override val name: String = "Instance-level Minimization"

    override suspend fun apply(
        context: HeavyIJDDContext,
        executor: MinimizationStageExecutor,
    ): Either<MinimizationError, IJDDContext> = executor.executeDeclarationLevelStage(context, this)
}
