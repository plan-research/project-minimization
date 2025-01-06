package org.plan.research.minimization.core.model.graph

import org.plan.research.minimization.core.model.DDItem

interface Graph<V : DDItem, E : GraphEdge<V>, G : Graph<V, E, G>> {
    val vertices: Collection<V>
    val edges: Collection<E>
    fun induce(cut: GraphCut<V>): G
}
