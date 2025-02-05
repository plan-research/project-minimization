package org.plan.research.minimization.core.algorithm.dd.impl.graph

import org.plan.research.minimization.core.model.DDItem

import arrow.core.getOrNone
import org.jgrapht.Graph
import org.jgrapht.traverse.TopologicalOrderIterator

internal class DepthCounter<V> private constructor(private val map: Map<V, Int>) where V : DDItem {
    val maxDepth = map.values.max()
    fun getDepthOf(v: V) = map.getOrNone(v)

    companion object {
        fun <V, E> create(graph: Graph<V, E>): DepthCounter<V> where V : DDItem {
            val topologicalSort = Iterable { TopologicalOrderIterator(graph) }.reversed()

            val map = buildMap<V, Int> {
                for (vertex in topologicalSort) {
                    val newDepth = getOrPut(vertex) { 0 } + 1
                    graph.outgoingEdgesOf(vertex).forEach { edge ->
                        compute(graph.getEdgeTarget(edge)) { _, depth ->
                            (depth ?: -1).coerceAtLeast(newDepth)
                        }
                    }
                }
            }
            return DepthCounter(map)
        }
    }
}
