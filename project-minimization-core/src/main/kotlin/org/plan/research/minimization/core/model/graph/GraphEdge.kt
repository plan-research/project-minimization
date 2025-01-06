package org.plan.research.minimization.core.model.graph

import org.plan.research.minimization.core.model.DDItem

abstract class GraphEdge<V : DDItem> {
    abstract val from: V
    abstract val to: V
    open operator fun component1(): V = from
    open operator fun component2(): V = to
}
