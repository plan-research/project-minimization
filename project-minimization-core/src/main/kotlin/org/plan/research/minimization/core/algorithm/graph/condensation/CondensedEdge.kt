package org.plan.research.minimization.core.algorithm.graph.condensation

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.GraphEdge

data class CondensedEdge<V : DDItem, E : GraphEdge<V>>(
    override val from: CondensedVertex<V, E>,
    override val to: CondensedVertex<V, E>,
    val originalEdges: List<E>,
) : GraphEdge<CondensedVertex<V, E>>() {
    override fun toString(): String = "Edge($from => $to)"
}
