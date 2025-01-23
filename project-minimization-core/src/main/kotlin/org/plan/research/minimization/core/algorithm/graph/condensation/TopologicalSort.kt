package org.plan.research.minimization.core.algorithm.graph.condensation

import org.plan.research.minimization.core.algorithm.graph.DepthFirstGraphWalkerVoid
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.core.model.graph.GraphWithAdjacencyList

/**
 * Topologically sorts the graph using classical DFS algorithm.
 *
 * The invariant of the result is
 * that for a graph without cycles, all the edges will point from the end to the begging of the list.
 *
 */
class TopologicalSort<V : DDItem, E : GraphEdge<V>, G : GraphWithAdjacencyList<V, E, G>> :
    DepthFirstGraphWalkerVoid<V, E, G, List<V>>() {
    private val sortedList = mutableListOf<V>()
    override suspend fun onUnvisitedNode(graph: G, node: V) {
        super.onUnvisitedNode(graph, node)
        sortedList.add(node)
    }

    override suspend fun onComplete(graph: G): List<V> = sortedList.reversed()
}
