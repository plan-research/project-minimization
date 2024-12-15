package org.plan.research.minimization.core.algorithm.graph.condensation

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.Graph
import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.core.model.graph.GraphWithAdjacencyList

import arrow.core.getOrElse
import arrow.core.getOrNone

typealias AdjacencyList<V, E> = Map<CondensedVertex<V>, List<CondensedEdge<V, E>>>

class CondensedGraph<V : DDItem, E : GraphEdge<V>, G : Graph<V, E>> internal constructor(
    val originalGraph: G,
    override val vertices: List<CondensedVertex<V>>,
    private val adjacencyList: AdjacencyList<V, E>,
) : GraphWithAdjacencyList<CondensedVertex<V>, CondensedEdge<V, E>>() {
    private val inDegrees = adjacencyList
        .values
        .flatten()
        .groupBy(keySelector = { it.to })
        .mapValues { it.value.size }
    override fun edgesFrom(vertex: CondensedVertex<V>) =
        adjacencyList.getOrNone(vertex)

    override fun outDegreeOf(vertex: CondensedVertex<V>): Int = edgesFrom(vertex).getOrElse { emptyList() }.size
    override fun inDegreeOf(vertex: CondensedVertex<V>): Int = inDegrees.getOrNone(vertex).getOrElse { 0 }

    companion object {
        fun <V : DDItem, E : GraphEdge<V>, G : GraphWithAdjacencyList<V, E>> from(condensedVertexSet: CondensedVertexSet<V, E, G>) =
            CondensedGraph(
                originalGraph = condensedVertexSet.originalGraph,
                vertices = condensedVertexSet.components,
                adjacencyList = condensedVertexSet
                    .components
                    .associate { it to condensedVertexSet.getAdjacentComponents(it) },
            )
    }
}
