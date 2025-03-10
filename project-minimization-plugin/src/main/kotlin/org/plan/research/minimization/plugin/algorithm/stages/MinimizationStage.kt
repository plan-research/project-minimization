package org.plan.research.minimization.plugin.algorithm.stages

import org.plan.research.minimization.plugin.algorithm.MinimizationError
import org.plan.research.minimization.plugin.context.HeavyIJDDContext
import org.plan.research.minimization.plugin.context.IJDDContextBase

import arrow.core.Either

typealias MinimizationResult = Either<MinimizationError, IJDDContextBase<*>>

interface MinimizationStage {
    val stageName: String

    suspend fun executeStage(context: HeavyIJDDContext<*>): MinimizationResult
}
