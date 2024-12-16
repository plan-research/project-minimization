package org.plan.research.minimization.core.algorithm.graph

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.core.model.graph.GraphWithAdjacencyList

import arrow.core.getOrNone

internal class TransposedGraph<V : DDItem, E : GraphEdge<V>, G : GraphWithAdjacencyList<V, E>>(
    val originalGraph: G,
    override val vertices: List<V> = originalGraph.vertices.toList(),
) : GraphWithAdjacencyList<V, TransposedGraph.TransposedEdge<V>>() {
    @Suppress("TYPE_ALIAS")
    private val adjacencyLists: Map<V, List<TransposedEdge<V>>> = buildMap<V, MutableList<TransposedEdge<V>>> {
        vertices.forEach { vertex ->
            val adjacent = originalGraph.edgesFrom(vertex)
            adjacent.onSome { edges ->
                edges.forEach { edge ->
                    val transposedEdge = TransposedEdge(vertex)
                    this[edge.to]?.add(transposedEdge) ?: put(edge.to, mutableListOf(transposedEdge))
                }
            }
        }
    }

    override fun outDegreeOf(vertex: V): Int = originalGraph.inDegreeOf(vertex)
    override fun inDegreeOf(vertex: V): Int = originalGraph.outDegreeOf(vertex)

    override fun edgesFrom(vertex: V) = adjacencyLists.getOrNone(vertex)
    internal class TransposedEdge<V : DDItem>(override val to: V) : GraphEdge<V>
}
