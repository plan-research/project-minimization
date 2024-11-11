package org.plan.research.minimization.core.algorithm.slicing

import org.plan.research.minimization.core.model.SlicingGraphNode

sealed interface SlicingError {
    data class MultipleRoots<T: SlicingGraphNode>(val roots: List<T>): SlicingError
}