package org.plan.research.minimization.core.algorithm.graph

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.core.model.graph.GraphWithAdjacencyList

import arrow.core.getOrElse

abstract class DepthFirstGraphWalker<V : DDItem, E : GraphEdge<V>, G : GraphWithAdjacencyList<V, E, G>, R, D : Any> {
    private val visited = mutableSetOf<V>()
    suspend fun visitGraph(graph: G): R {
        graph.vertices.forEach { processComponent(graph, it) }
        return onComplete(graph)
    }

    private suspend fun processComponent(graph: G, startingVertex: V) {
        if (startingVertex !in visited) {
            val data = onNewVisitedComponent(graph, startingVertex)
            visited.add(startingVertex)
            onUnvisitedNode(graph, startingVertex, data)
        }
    }

    suspend fun visitComponent(graph: G, startingVertex: V): R {
        processComponent(graph, startingVertex)
        return onComplete(graph)
    }

    protected open suspend fun onUnvisitedNode(graph: G, node: V, data: D) {
        val edgesFrom = graph.edgesFrom(node).getOrElse { return@onUnvisitedNode }
        for (edge in edgesFrom) {
            if (edge.to !in visited) {
                visited.add(edge.to)
                onUnvisitedNode(graph, edge.to, data)
            }
            onPassedEdge(graph, edge, data)
        }
    }

    protected abstract suspend fun onComplete(graph: G): R

    protected open suspend fun onPassedEdge(graph: G, edge: E, data: D) = Unit
    protected abstract suspend fun onNewVisitedComponent(graph: G, startingVertex: V): D
}

abstract class DepthFirstGraphWalkerVoid<V : DDItem, E : GraphEdge<V>, G : GraphWithAdjacencyList<V, E, G>, R> :
    DepthFirstGraphWalker<V, E, G, R, Unit>() {
    protected open suspend fun onUnvisitedNode(graph: G, node: V) = super.onUnvisitedNode(graph, node, Unit)
    protected open suspend fun onPassedEdge(graph: G, edge: E) = super.onPassedEdge(graph, edge, Unit)
    override suspend fun onNewVisitedComponent(graph: G, startingVertex: V) = Unit

    final override suspend fun onUnvisitedNode(graph: G, node: V, data: Unit) = onUnvisitedNode(graph, node)
    final override suspend fun onPassedEdge(graph: G, edge: E, data: Unit) = onPassedEdge(graph, edge)
}
