package org.plan.research.minimization.core.algorithm.graph

import org.plan.research.minimization.core.model.DDContext
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.Graph
import org.plan.research.minimization.core.model.graph.GraphEdge

interface GraphContext<V : DDItem, E : GraphEdge<V>, G : Graph<V, E>> : DDContext {
    val graph: G
    val currentLevel: List<DDItem>?
    fun copy(currentLevel: List<DDItem>? = null): GraphContext<V, E, G>
}
