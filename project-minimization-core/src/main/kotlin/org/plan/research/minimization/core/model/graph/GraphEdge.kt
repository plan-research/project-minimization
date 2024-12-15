package org.plan.research.minimization.core.model.graph

import org.plan.research.minimization.core.model.DDItem

interface GraphEdge<V : DDItem> {
    val to: V
}
