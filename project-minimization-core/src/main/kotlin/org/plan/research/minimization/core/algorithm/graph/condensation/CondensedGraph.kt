package org.plan.research.minimization.core.algorithm.graph.condensation

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.core.model.graph.GraphWithAdjacencyList

import arrow.core.getOrElse
import arrow.core.getOrNone

typealias AdjacencyList<V, E> = Map<CondensedVertex<V>, List<CondensedEdge<V, E>>>

class CondensedGraph<V : DDItem, E : GraphEdge<V>> internal constructor(
    override val vertices: List<CondensedVertex<V>>,
    private val adjacencyList: AdjacencyList<V, E>,
) : GraphWithAdjacencyList<CondensedVertex<V>, CondensedEdge<V, E>>() {
    private val inDegrees = adjacencyList
        .values
        .flatten()
        .groupBy(keySelector = { it.to })
        .mapValues { it.value.size }

    @Suppress("TYPE_ALIAS")
    private val reverseAdjacencyList: AdjacencyList<V, E> =
        buildMap<CondensedVertex<V>, MutableList<CondensedEdge<V, E>>> {
            adjacencyList.forEach { (vertex, edges) ->
                edges.forEach { edge ->
                    getOrPut(edge.to) { mutableListOf() }.add(edge)
                }
            }
        }

    override fun edgesFrom(vertex: CondensedVertex<V>) = adjacencyList.getOrNone(vertex)

    internal fun edgesTo(vertex: CondensedVertex<V>) = reverseAdjacencyList.getOrNone(vertex)

    override fun outDegreeOf(vertex: CondensedVertex<V>): Int = edgesFrom(vertex).getOrElse { emptyList() }.size
    override fun inDegreeOf(vertex: CondensedVertex<V>): Int = inDegrees.getOrNone(vertex).getOrElse { 0 }

    internal fun withoutNodes(nodesList: Set<CondensedVertex<V>>): CondensedGraph<V, E> = CondensedGraph(
        vertices = vertices.filter { it !in nodesList },
        adjacencyList = adjacencyList.filterKeys { it !in nodesList }
            .mapValues { (_, edges) -> edges.filter { it.to !in nodesList } },
    )

    override fun toString(): String = "CondensedGraph(vertices=$vertices, adjacencyList=$adjacencyList)"

    companion object {
        fun <V : DDItem, E : GraphEdge<V>, G : GraphWithAdjacencyList<V, E>> from(condensedVertexSet: CondensedVertexSet<V, E, G>) =
            CondensedGraph(
                vertices = condensedVertexSet.components,
                adjacencyList = condensedVertexSet
                    .components
                    .associate { it to condensedVertexSet.getAdjacentComponents(it) },
            )
    }
}
