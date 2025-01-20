package org.plan.research.minimization.core.algorithm.graph

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.GraphCut
import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.core.model.graph.GraphWithAdjacencyList

internal class TransposedGraph<V : DDItem, E : GraphEdge<V>, G : GraphWithAdjacencyList<V, E, G>>(
    val originalGraph: G,
    override val vertices: List<V> = originalGraph.vertices.toList(),
) : GraphWithAdjacencyList<V, TransposedGraph.TransposedEdge<V>, TransposedGraph<V, E, G>>() {
    override val edges: List<TransposedEdge<V>> = originalGraph.edges.map { TransposedEdge(it.to, it.from) }.toList()
    override fun induce(cut: GraphCut<V>): TransposedGraph<V, E, G> = TransposedGraph(originalGraph.induce(cut))

    override fun inDegreeOf(vertex: V): Int = originalGraph.outDegreeOf(vertex)

    internal class TransposedEdge<V : DDItem>(override val from: V, override val to: V) : GraphEdge<V>()
}
