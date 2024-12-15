package org.plan.research.minimization.core.algorithm.graph.condensation

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.GraphEdge

data class CondensedEdge<V : DDItem, E : GraphEdge<V>>(
    val from: CondensedVertex<V>,
    override val to: CondensedVertex<V>,
    val originalEdges: List<E>,
) : GraphEdge<CondensedVertex<V>>
