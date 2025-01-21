package org.plan.research.minimization.core.algorithm.dd.impl.graph

import org.jgrapht.Graph
import org.jgrapht.graph.AsSubgraph
import org.jgrapht.graph.EdgeReversedGraph
import org.jgrapht.traverse.DepthFirstIterator
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.GraphCut


class LayerToCutTransformer<T : DDItem, E> {
    data class LayerToCutTransformerResult<T: DDItem, E>(
        val retainedCut: GraphCut<T, E>,
        val deletedCut: GraphCut<T, E>,
    )

    fun transform(
        originalGraph: Graph<T, E>,
        graph: Graph<T, E>,
        deletedItems: List<T>,
    ): LayerToCutTransformerResult<T, E> {
        val edgeReversedGraph = EdgeReversedGraph(graph)
        val depthFirstIterator = DepthFirstIterator(edgeReversedGraph, deletedItems)

        val itemsToDelete = mutableSetOf<T>()
        for (deletedItem in depthFirstIterator) {
            itemsToDelete.add(deletedItem)
        }

        val deletedCut = AsSubgraph(originalGraph, itemsToDelete)
        val retainedCut = AsSubgraph(originalGraph, graph.vertexSet() - itemsToDelete)

        return LayerToCutTransformerResult(retainedCut, deletedCut)
    }
}
