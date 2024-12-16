package org.plan.research.minimization.core.algorithm.graph.condensation

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.core.model.graph.GraphWithAdjacencyList

import arrow.core.getOrElse
import arrow.core.getOrNone
import arrow.core.memoize
import arrow.core.raise.option

class CondensedVertexSet<V : DDItem, E : GraphEdge<V>, G : GraphWithAdjacencyList<V, E>> internal constructor(
    val components: List<CondensedVertex<V>>,
    val originalGraph: G,
) {
    @Suppress("TYPE_ALIAS")
    private val componentByVertex: Map<V, CondensedVertex<V>> = components
        .flatMap { component -> component.underlyingVertexes.map { it to component } }
        .toMap()
    private val adjacentComponentsCache = ::computeAdjacentSets.memoize()

    fun getComponent(vertex: V) = componentByVertex.getOrNone(vertex)
    private fun computeAdjacentSets(component: CondensedVertex<V>): List<CondensedEdge<V, E>> = component
        .underlyingVertexes
        .asSequence()
        .flatMap {
            option {
                originalGraph
                    .edgesFrom(it)
                    .bind()
                    .mapNotNull {
                        val toComponent = getComponent(it.to).getOrNull().takeIf { component !== it } ?: return@mapNotNull null
                        toComponent to it
                    }
            }.getOrElse { emptyList() }
        }
        .groupBy(keySelector = { it.first }, valueTransform = { it.second })
        .map { (toComponent, edges) -> CondensedEdge(component, toComponent, edges) }
    fun getAdjacentComponents(component: CondensedVertex<V>): List<CondensedEdge<V, E>> =
        adjacentComponentsCache(component)
}
