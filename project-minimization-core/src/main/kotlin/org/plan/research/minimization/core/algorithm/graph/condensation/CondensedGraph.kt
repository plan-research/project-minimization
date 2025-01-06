package org.plan.research.minimization.core.algorithm.graph.condensation

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.GraphCut
import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.core.model.graph.GraphWithAdjacencyList

import arrow.core.getOrElse
import arrow.core.getOrNone

internal typealias AdjacencyList<V, E> = Map<CondensedVertex<V>, List<CondensedEdge<V, E>>>
private typealias CondensedCut<V> = GraphCut<CondensedVertex<V>>

class CondensedGraph<V : DDItem, E : GraphEdge<V>> internal constructor(
    override val vertices: List<CondensedVertex<V>>,
    @Suppress("TYPE_ALIAS") override val edges: Collection<CondensedEdge<V, E>>,
) : GraphWithAdjacencyList<CondensedVertex<V>, CondensedEdge<V, E>, CondensedGraph<V, E>>() {
    private val inDegrees = adjacencyList
        .values
        .flatten()
        .groupBy(keySelector = { it.to })
        .mapValues { it.value.size }

    @Suppress("TYPE_ALIAS")
    private val reverseAdjacencyList: AdjacencyList<V, E> = edges.groupBy(keySelector = { it.to })

    internal fun edgesTo(vertex: CondensedVertex<V>) = reverseAdjacencyList.getOrNone(vertex)

    override fun inDegreeOf(vertex: CondensedVertex<V>): Int = inDegrees.getOrNone(vertex).getOrElse { 0 }

    internal fun withoutNodes(nodesList: Set<CondensedVertex<V>>): CondensedGraph<V, E> = CondensedGraph(
        vertices = vertices.filter { it !in nodesList },
        edges = edges.filter { it.to !in nodesList && it.from !in nodesList },
    )

    override fun toString(): String = "CondensedGraph(vertices=$vertices, adjacencyList=$adjacencyList)"
    override fun induce(cut: CondensedCut<V>) = withoutNodes(vertices.toSet() - cut.selectedVertices)
}
