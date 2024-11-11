package org.plan.research.minimization.core.algorithm.slicing.impl

import arrow.core.raise.either
import org.plan.research.minimization.core.algorithm.slicing.SlicingAlgorithm
import org.plan.research.minimization.core.algorithm.slicing.SlicingResult
import org.plan.research.minimization.core.model.SlicingGraph
import org.plan.research.minimization.core.model.SlicingGraphNode

class SlicingImpl<T: SlicingGraphNode>: SlicingAlgorithm<T> {
    override suspend fun slice(roots: List<T>): SlicingResult<T> = either {
        val graphTraversal = SlicingGraphTraversal<T>()
        graphTraversal.visitAll(roots)

        SlicingGraph(graphTraversal.visited.toList())
    }
}