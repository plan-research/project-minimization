package org.plan.research.minimization.core.model.graph

import org.plan.research.minimization.core.model.DDItem

data class GraphCut<V>(val selectedVertices: List<V>)
where V : DDItem
