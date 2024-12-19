package org.plan.research.minimization.core.model.graph

import org.plan.research.minimization.core.model.DDItem

interface Graph<V : DDItem, E : GraphEdge<V>> {
    val vertices: Collection<V>
}
