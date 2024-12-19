package org.plan.research.minimization.core.algorithm.graph

import org.plan.research.minimization.core.algorithm.graph.condensation.CondensedGraph
import org.plan.research.minimization.core.model.DDContext
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.core.model.graph.GraphWithAdjacencyList

interface GraphContext<V : DDItem, E : GraphEdge<V>, G : GraphWithAdjacencyList<V, E>> : DDContext {
    val graph: G?
    val condensedGraph: CondensedGraph<V, E>?
    val currentLevel: List<DDItem>?
    fun copy(
        currentLevel: List<DDItem>? = this.currentLevel,
        condensedGraph: CondensedGraph<V, E>? = this.condensedGraph,
    ): GraphContext<V, E, G>
}
