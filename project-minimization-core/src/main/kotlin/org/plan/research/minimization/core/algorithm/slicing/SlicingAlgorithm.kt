package org.plan.research.minimization.core.algorithm.slicing

import arrow.core.Either
import org.plan.research.minimization.core.model.SlicingGraph
import org.plan.research.minimization.core.model.SlicingGraphNode

typealias SlicingResult<T> = Either<SlicingError, SlicingGraph<T>>

interface SlicingAlgorithm<T: SlicingGraphNode> {
    suspend fun slice(roots: List<T>): SlicingResult<T>
}
