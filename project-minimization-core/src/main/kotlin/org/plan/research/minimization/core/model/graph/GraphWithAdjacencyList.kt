package org.plan.research.minimization.core.model.graph

import org.plan.research.minimization.core.model.DDItem

import arrow.core.Option

abstract class GraphWithAdjacencyList<V : DDItem, E : GraphEdge<V>> : Graph<V, E> {
    val sources: List<V> by lazy { vertices.filter { outDegreeOf(it) == 0 } }
    val sinks: List<V> by lazy { vertices.filter { outDegreeOf(it) == 0 } }
    abstract fun inDegreeOf(vertex: V): Int
    abstract fun outDegreeOf(vertex: V): Int
    abstract fun edgesFrom(vertex: V): Option<List<E>>
}
