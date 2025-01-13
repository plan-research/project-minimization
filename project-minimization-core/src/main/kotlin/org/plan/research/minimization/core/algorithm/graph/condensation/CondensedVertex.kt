package org.plan.research.minimization.core.algorithm.graph.condensation

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.GraphEdge

data class CondensedVertex<V : DDItem, E : GraphEdge<V>>(
    val underlyingVertexes: List<V>,
    val edgesInCondensed: List<E>
) : DDItem
