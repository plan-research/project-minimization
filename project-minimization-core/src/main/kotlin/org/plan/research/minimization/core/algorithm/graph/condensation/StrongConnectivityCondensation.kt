package org.plan.research.minimization.core.algorithm.graph.condensation

import org.plan.research.minimization.core.algorithm.graph.DepthFirstGraphWalker
import org.plan.research.minimization.core.algorithm.graph.TransposedGraph
import org.plan.research.minimization.core.algorithm.graph.TransposedGraph.TransposedEdge
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.core.model.graph.GraphWithAdjacencyList

import arrow.core.filterOption
import arrow.core.getOrNone
import arrow.core.raise.option

import kotlin.collections.component1
import kotlin.collections.component2

object StrongConnectivityCondensation {
    suspend fun <V : DDItem, E : GraphEdge<V>, G : GraphWithAdjacencyList<V, E>> compressGraph(graph: G): CondensedGraph<V, E> {
        val topologicalSortedVertices = TopologicalSort<V, E, G>().visitGraph(graph)
        val transposedGraph = TransposedGraph(graph, topologicalSortedVertices)
        return GraphCondenser<V, E, G>().visitGraph(transposedGraph)
    }

    private class GraphCondenser<V : DDItem, E : GraphEdge<V>, G : GraphWithAdjacencyList<V, E>> :
        DepthFirstGraphWalker<V, TransposedEdge<V>, TransposedGraph<V, E, G>, CondensedGraph<V, E>, MutableList<V>>() {
        private val currentComponents = mutableListOf<MutableList<V>>()

        override suspend fun onComplete(graph: TransposedGraph<V, E, G>): CondensedGraph<V, E> {
            val vertices = currentComponents.map { CondensedVertex(it) }
            val vertexToComponent = vertices
                .flatMap { component -> component.underlyingVertexes.map { it to component } }
                .toMap()
            val edges = graph
                .originalGraph
                .edges
                .asSequence()
                .map { edge ->
                    option {
                        val (from, to) = edge
                        val fromComponent = vertexToComponent.getOrNone(from).bind()
                        val toComponent = vertexToComponent.getOrNone(to).bind()
                        ensure(fromComponent !== toComponent)
                        (fromComponent to toComponent) to edge
                    }
                }
                .filterOption()
                .groupBy(keySelector = { it.first }, valueTransform = { it.second })
                .map { (fromTo, edges) ->
                    val (from, to) = fromTo
                    CondensedEdge(from, to, edges)
                }
            return CondensedGraph<V, E>(
                vertices = vertices,
                edges = edges,
            )
        }

        override suspend fun onNewVisitedComponent(
            graph: TransposedGraph<V, E, G>,
            startingVertex: V,
        ): MutableList<V> {
            currentComponents.add(mutableListOf())
            return currentComponents.last()
        }

        override suspend fun onUnvisitedNode(graph: TransposedGraph<V, E, G>, node: V, data: MutableList<V>) {
            data.add(node)
            super.onUnvisitedNode(graph, node, data)
        }
    }
}
