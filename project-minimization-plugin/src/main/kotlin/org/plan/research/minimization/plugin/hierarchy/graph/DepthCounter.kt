package org.plan.research.minimization.plugin.hierarchy.graph

import org.plan.research.minimization.core.algorithm.graph.condensation.TopologicalSort
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.core.model.graph.GraphWithAdjacencyList

import arrow.core.getOrNone

internal class DepthCounter<V>(private val map: Map<V, Int>) where V : DDItem {
    val maxDepth = map.values.max()
    fun getDepthOf(v: V) = map.getOrNone(v)

    /**
     * Gets the percentage of how depth is the vertex according to the
     *
     * @param v
     */
    fun getPercent(v: V) = getDepthOf(v).map { it * 100 / maxDepth }

    companion object {
        suspend fun <V, E, G> create(graph: G): DepthCounter<V> where V : DDItem, E : GraphEdge<V>, G : GraphWithAdjacencyList<V, E, G> {
            val topologicalSort = TopologicalSort<V, E, G>().visitGraph(graph).reversed()

            val map = buildMap {
                for (vertex in topologicalSort) {
                    val currentDepth = getOrPut(vertex) { 0 }
                    graph.edgesFrom(vertex).onSome {
                        it.forEach { edge ->
                            compute(edge.to) { _, depth -> (depth ?: -1).coerceAtLeast(currentDepth + 1) }
                        }
                    }
                }
            }
            return DepthCounter(map)
        }
    }
}
