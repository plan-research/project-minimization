package org.plan.research.minimization.core.algorithm.graph.hierarchical

import org.plan.research.minimization.core.algorithm.graph.GraphContext
import org.plan.research.minimization.core.algorithm.graph.condensation.CondensedGraph
import org.plan.research.minimization.core.algorithm.graph.condensation.CondensedVertex
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.core.model.graph.GraphWithAdjacencyList

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

internal typealias GraphHierarchyInactiveRefCounter<V> = PersistentMap<CondensedVertex<V>, Int>

internal class GraphHierarchicalDDContext<V, E, G, C>(
    val backingContext: C,
    val inactiveElements: GraphHierarchyInactiveRefCounter<V> = persistentMapOf(),
) : GraphContext<V, E, G>
where V : DDItem,
E : GraphEdge<V>,
G : GraphWithAdjacencyList<V, E>,
C : GraphContext<V, E, G> {
    override val graph: G = requireNotNull(backingContext.graph)
    override val currentLevel: List<DDItem>?
        get() = backingContext.currentLevel
    override val condensedGraph: CondensedGraph<V, E>?
        get() = backingContext.condensedGraph

    fun copy(
        currentLevel: List<DDItem>? = null,
        inactiveElements: GraphHierarchyInactiveRefCounter<V> = this.inactiveElements,
        condensedGraph: CondensedGraph<V, E>? = this.condensedGraph,
    ) = GraphHierarchicalDDContext<V, E, G, C>(
        backingContext.copy(currentLevel = currentLevel, condensedGraph = condensedGraph) as C,
        inactiveElements = inactiveElements,
    )

    override fun copy(currentLevel: List<DDItem>?, condensedGraph: CondensedGraph<V, E>?): GraphContext<V, E, G> = copy(
        currentLevel = currentLevel,
        inactiveElements = this.inactiveElements,
        condensedGraph = condensedGraph,
    )

    fun copy(backingContext: C) = GraphHierarchicalDDContext<V, E, G, C>(
        backingContext = backingContext,
        inactiveElements = this.inactiveElements,
    )
}
