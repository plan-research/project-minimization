package org.plan.research.minimization.core.algorithm.graph.condensation

import org.plan.research.minimization.core.algorithm.graph.DepthFirstGraphWalker
import org.plan.research.minimization.core.algorithm.graph.TransposedGraph
import org.plan.research.minimization.core.algorithm.graph.TransposedGraph.TransposedEdge
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.core.model.graph.GraphWithAdjacencyList

object StrongConnectivityCondensation {
    fun <V : DDItem, E : GraphEdge<V>, G : GraphWithAdjacencyList<V, E>> compressGraph(graph: G): CondensedVertexSet<V, E, G> {
        val topologicalSortedVertices = TopologicalSort<V, E, G>().visitGraph(graph)
        val transposedGraph = TransposedGraph(graph, topologicalSortedVertices)
        return GraphCondenser<V, E, G>().visitGraph(transposedGraph)
    }

    private class GraphCondenser<V : DDItem, E : GraphEdge<V>, G : GraphWithAdjacencyList<V, E>> :
        DepthFirstGraphWalker<V, TransposedEdge<V>, TransposedGraph<V, E, G>, CondensedVertexSet<V, E, G>, MutableList<V>>() {
        private val currentComponents = mutableListOf<MutableList<V>>()

        override fun onComplete(graph: TransposedGraph<V, E, G>): CondensedVertexSet<V, E, G> =
            CondensedVertexSet(
                components = currentComponents.map { CondensedVertex(it) },
                originalGraph = graph.originalGraph,
            )

        override fun onNewVisitedComponent(
            graph: TransposedGraph<V, E, G>,
            startingVertex: V,
        ): MutableList<V> {
            currentComponents.add(mutableListOf())
            return currentComponents.last()
        }

        override fun onUnvisitedNode(graph: TransposedGraph<V, E, G>, node: V, data: MutableList<V>) {
            data.add(node)
            super.onUnvisitedNode(graph, node, data)
        }
    }
}
