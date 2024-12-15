package org.plan.research.minimization.core.algorithm.graph.condensation

import org.plan.research.minimization.core.algorithm.graph.DepthFirstGraphWalker
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.core.model.graph.GraphWithAdjacencyList

import arrow.core.getOrNone

object StrongConnectivityCondensation {
    fun <V : DDItem, E : GraphEdge<V>, G : GraphWithAdjacencyList<V, E>> compressGraph(graph: G): CondensedVertexSet<V, E, G> {
        val topologicalSortedVertices = TopologicalSort<V, E, G>().visitGraph(graph)
        val transposedGraph = TransposedGraphWithAdjacencyList(graph, topologicalSortedVertices)
        return GraphCondenser<V, E, G>().visitGraph(transposedGraph)
    }

    private class TransposedGraphWithAdjacencyList<V : DDItem, E : GraphEdge<V>, G : GraphWithAdjacencyList<V, E>>(
        val originalGraph: G,
        override val vertices: List<V>,
    ) :
        GraphWithAdjacencyList<V, GraphEdge<V>>() {
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
        private class TransposedEdge<V : DDItem>(override val to: V) : GraphEdge<V>
    }

    private class GraphCondenser<V : DDItem, E : GraphEdge<V>, G : GraphWithAdjacencyList<V, E>> :
        DepthFirstGraphWalker<V, GraphEdge<V>, TransposedGraphWithAdjacencyList<V, E, G>, CondensedVertexSet<V, E, G>, MutableList<V>>() {
        private val currentComponents = mutableListOf<MutableList<V>>()

        override fun onComplete(graph: TransposedGraphWithAdjacencyList<V, E, G>): CondensedVertexSet<V, E, G> =
            CondensedVertexSet(
                components = currentComponents.map { CondensedVertex(it) },
                originalGraph = graph.originalGraph,
            )

        override fun onNewVisitedComponent(
            graph: TransposedGraphWithAdjacencyList<V, E, G>,
            startingVertex: V,
        ): MutableList<V> {
            currentComponents.add(mutableListOf())
            return currentComponents.last()
        }

        override fun onUnvisitedNode(graph: TransposedGraphWithAdjacencyList<V, E, G>, node: V, data: MutableList<V>) {
            data.add(node)
            super.onUnvisitedNode(graph, node, data)
        }
    }
}
