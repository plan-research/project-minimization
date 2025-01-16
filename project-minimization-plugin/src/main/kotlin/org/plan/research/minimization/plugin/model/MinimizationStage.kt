package org.plan.research.minimization.plugin.model

import org.plan.research.minimization.plugin.errors.MinimizationError
import org.plan.research.minimization.plugin.model.context.HeavyIJDDContext
import org.plan.research.minimization.plugin.model.context.IJDDContextBase
import org.plan.research.minimization.plugin.model.state.DDStrategy

import arrow.core.Either
import arrow.optics.optics
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.Tag

typealias MinimizationResult = Either<MinimizationError, IJDDContextBase<*>>

/**
 * Interface representing an executor responsible for handling various stages of the minimization process.
 */
interface MinimizationStageExecutor {
    suspend fun executeFileLevelStage(
        context: HeavyIJDDContext<*>,
        fileLevelStage: FileLevelStage,
    ): MinimizationResult

    suspend fun executeFunctionLevelStage(
        context: HeavyIJDDContext<*>,
        functionLevelStage: FunctionLevelStage,
    ): MinimizationResult

    suspend fun executeDeclarationLevelStage(
        context: HeavyIJDDContext<*>,
        declarationLevelStage: DeclarationLevelStage,
    ): MinimizationResult
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
        context: HeavyIJDDContext<*>,
        executor: MinimizationStageExecutor,
    ): MinimizationResult
}

/**
 * Represents a stage in the minimization process that operates at the file level.
 *
 * This class configures and applies a hierarchical delta debugging algorithm to minimize a project's file structure.
 *
 * @property ddAlgorithm The delta debugging algorithm to use for minimization.
 */
@Tag("fileLevelStage")
@optics
data class FileLevelStage(
    @Property val ddAlgorithm: DDStrategy = DDStrategy.PROBABILISTIC_DD,
) : MinimizationStage {
    override val name: String = "File-Level Minimization"

    override suspend fun apply(context: HeavyIJDDContext<*>, executor: MinimizationStageExecutor) =
        executor.executeFileLevelStage(context, this)

    companion object
}

@Tag("functionLevelStage")
@optics
data class FunctionLevelStage(
    @Property val ddAlgorithm: DDStrategy = DDStrategy.PROBABILISTIC_DD,
) : MinimizationStage {
    override val name: String = "Body Replacement Algorithm"

    override suspend fun apply(
        context: HeavyIJDDContext<*>,
        executor: MinimizationStageExecutor,
    ): MinimizationResult = executor.executeFunctionLevelStage(context, this)

    companion object
}

@Tag("declarationLevelStage")
@optics
data class DeclarationLevelStage(
    @Property val ddAlgorithm: DDStrategy = DDStrategy.PROBABILISTIC_DD,
    @Property val depthThreshold: Int = 2,
) : MinimizationStage {
    override val name: String = "Instance-level Minimization"

    override suspend fun apply(
        context: HeavyIJDDContext<*>,
        executor: MinimizationStageExecutor,
    ): MinimizationResult = executor.executeDeclarationLevelStage(context, this)

    companion object
}
