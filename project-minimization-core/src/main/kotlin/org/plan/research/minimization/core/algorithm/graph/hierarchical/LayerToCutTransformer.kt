package org.plan.research.minimization.core.algorithm.graph.hierarchical

import org.plan.research.minimization.core.model.DDContextWithLevel
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.Graph
import org.plan.research.minimization.core.model.graph.GraphCut
import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDDGenerator

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
 * @param C Type of the context, which must extend [DDContextWithLevel].
 */
interface LayerToCutTransformer<V, E, G, C> where V : DDItem, E : GraphEdge<V>, G : Graph<V, E, G>, C : DDContextWithLevel<C> {
    /**
     * Transforms a given hierarchical graph layer into a corresponding graph cut based on the provided context.
     *
     * @param layer A list of vertices representing the graph layer to be transformed.
     * The implementation assumes that no vertex is a predecessor of any other vertex
     * @param context The contextual information required for the transformation
     * @return A [GraphCut] object containing the selected vertices resulting from the transformation.
     */
    suspend fun transform(layer: List<V>, context: C): GraphCut<V>

    /**
     * Sets the current graph layer to the specified list of vertices.
     *
     * @param layer A list of vertices representing the selected by [GraphLayerHierarchyProducer]] the graph layer to be set.
     * This layer is used as the current working layer for later transformations
     * or operations.
     */
    @Deprecated("Temporary solution that will be removed in the future")
    suspend fun setCurrentLayer(layer: List<V>)
}
