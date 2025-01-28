package org.plan.research.minimization.core.algorithm.dd.impl.graph

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.GraphCut

import org.jgrapht.Graph
import org.jgrapht.graph.EdgeReversedGraph
import org.jgrapht.traverse.DepthFirstIterator

/**
 * Class that defines the transformation of a hierarchical graph layer
 * into a corresponding graph cut.
 *
 * The only user of that object is [GraphLayerHierarchyGenerator].
 */
class LayerToCutTransformer<T : DDItem> {
    fun transform(
        graph: Graph<T, *>,
        deletedItems: List<T>,
    ): LayerToCutTransformerResult<T> {
        val edgeReversedGraph = EdgeReversedGraph(graph)
        val depthFirstIterator = DepthFirstIterator(edgeReversedGraph, deletedItems)

        val deletedCut = mutableSetOf<T>()
        for (deletedItem in depthFirstIterator) {
            deletedCut.add(deletedItem)
        }

        val retainedCut = graph.vertexSet() - deletedCut

        return LayerToCutTransformerResult(retainedCut, deletedCut)
    }
    data class LayerToCutTransformerResult<T : DDItem>(
        val retainedCut: GraphCut<T>,
        val deletedCut: GraphCut<T>,
    )
}
