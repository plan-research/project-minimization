package org.plan.research.minimization.core.algorithm.graph.condensation

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.GraphCut
import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.core.model.graph.GraphWithAdjacencyList

import arrow.core.getOrElse
import arrow.core.getOrNone

internal typealias AdjacencyList<V, E> = Map<CondensedVertex<V, E>, List<CondensedEdge<V, E>>>
private typealias CondensedCut<V, E> = GraphCut<CondensedVertex<V, E>>

class CondensedGraph<V : DDItem, E : GraphEdge<V>> internal constructor(
    override val vertices: List<CondensedVertex<V, E>>,
    @Suppress("TYPE_ALIAS") override val edges: Collection<CondensedEdge<V, E>>,
) : GraphWithAdjacencyList<CondensedVertex<V, E>, CondensedEdge<V, E>, CondensedGraph<V, E>>() {
    private val inDegrees = adjacencyList
        .values
        .flatten()
        .groupBy(keySelector = { it.to })
        .mapValues { it.value.size }

    @Suppress("TYPE_ALIAS")
    private val reverseAdjacencyList: AdjacencyList<V, E> = edges.groupBy(keySelector = { it.to })

    fun edgesTo(vertex: CondensedVertex<V, E>) = reverseAdjacencyList.getOrNone(vertex)

    override fun inDegreeOf(vertex: CondensedVertex<V, E>): Int = inDegrees.getOrNone(vertex).getOrElse { 0 }

    fun withoutNodes(nodesList: Set<CondensedVertex<V, E>>): CondensedGraph<V, E> = CondensedGraph(
        vertices = vertices.filter { it !in nodesList },
        edges = edges.filter { it.to !in nodesList && it.from !in nodesList },
    )

    override fun toString(): String = "CondensedGraph(vertices=$vertices, adjacencyList=$adjacencyList)"
    override fun induce(cut: CondensedCut<V, E>) = withoutNodes(vertices.toSet() - cut.selectedVertices)
}
