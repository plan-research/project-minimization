package org.plan.research.minimization.core.model.graph

import org.plan.research.minimization.core.model.DDItem

import arrow.core.getOrElse
import arrow.core.getOrNone

abstract class GraphWithAdjacencyList<V : DDItem, E : GraphEdge<V>> : Graph<V, E> {
    val sources: List<V> by lazy { vertices.filter { inDegreeOf(it) == 0 } }
    val sinks: List<V> by lazy { vertices.filter { outDegreeOf(it) == 0 } }
    protected val adjacencyList by lazy { edges.groupBy { it.from } }
    abstract fun inDegreeOf(vertex: V): Int
    open fun outDegreeOf(vertex: V): Int = edgesFrom(vertex).getOrElse { emptyList() }.size
    open fun edgesFrom(vertex: V) = adjacencyList.getOrNone(vertex)
}
