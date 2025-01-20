package org.plan.research.minimization.core.algorithm.graph.hierarchical

import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDDGenerator
import org.plan.research.minimization.core.algorithm.graph.DepthFirstGraphWalkerVoid
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.Graph
import org.plan.research.minimization.core.model.graph.GraphCut
import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.core.model.graph.GraphWithAdjacencyList

/**
 * Interface that defines the transformation of a hierarchical graph layer
 * into a corresponding graph cut.
 * These transformations often require a lot of contextual information, thus the context is given
 *
 * The only user of that object is [GraphLayerHierarchyProducer].
 * The [LayerToCutTransformer] provides middleware between linear
 * [HierarchicalDDGenerator] and cut-based [GraphLayerHierarchyProducer].
 *
 * **This solution is experimental and could be removed in the future**
 *
 * @param V Type of the vertices in the graph layer, which must extend [DDItem].
 * @param E Type of the edges in the graph, which must extend [GraphEdge].
 * @param G Type of the graph, which must implement [Graph].
 */
class LayerToCutTransformer<V, E, G> where V : DDItem, E : GraphEdge<V>, G : GraphWithAdjacencyList<V, E, G> {
    /**
     * Transforms a given hierarchical graph layer into a corresponding graph cut based on the provided context.
     *
     * @param layer A list of vertices representing the graph layer to be transformed.
     * The implementation assumes that no vertex is a predecessor of any other vertex
     * @param graph A graph on which the transformation should be applied
     * @return A [GraphCut] object containing the selected vertices resulting from the transformation.
     */
    suspend fun transform(graph: G, layer: List<V>): GraphCut<V> {
        val layerWithDependencies = DeletedDependenciesCollector(layer).visitGraph(graph)
        return GraphCut(layerWithDependencies)
    }

    private inner class DeletedDependenciesCollector(private val deletedElements: Set<V>) :
        DepthFirstGraphWalkerVoid<
    V,
    E,
    G,
    Set<V>>() {
        private val collectedElements = mutableSetOf<V>()

        constructor(elements: List<V>) : this(elements.toSet())
        override suspend fun onUnvisitedNode(graph: G, node: V) {
            if (node in deletedElements) {
                collectedElements.add(node)
            } else {
                super.onUnvisitedNode(graph, node)
            }
        }

        override suspend fun onPassedEdge(graph: G, edge: E) {
            if (edge.to in collectedElements) {
                collectedElements.add(edge.from)
            }
        }

        override suspend fun onComplete(graph: G) = collectedElements
    }
}
