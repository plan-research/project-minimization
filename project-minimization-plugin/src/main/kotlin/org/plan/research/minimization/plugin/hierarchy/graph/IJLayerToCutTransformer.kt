package org.plan.research.minimization.plugin.hierarchy.graph

import org.plan.research.minimization.core.algorithm.graph.DepthFirstGraphWalkerVoid
import org.plan.research.minimization.core.algorithm.graph.condensation.CondensedEdge
import org.plan.research.minimization.core.algorithm.graph.condensation.CondensedGraph
import org.plan.research.minimization.core.algorithm.graph.condensation.CondensedVertex
import org.plan.research.minimization.core.algorithm.graph.hierarchical.LayerToCutTransformer
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.GraphCut
import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.PsiStubDDItem
import org.plan.research.minimization.plugin.psi.graph.CondensedInstanceLevelEdge
import org.plan.research.minimization.plugin.psi.graph.CondensedInstanceLevelGraph
import org.plan.research.minimization.plugin.psi.graph.CondensedInstanceLevelNode
import org.plan.research.minimization.plugin.psi.graph.PsiIJEdge
import org.plan.research.minimization.core.algorithm.graph.hierarchical.GraphLayerHierarchyProducer
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm

/**
 * A singleton object implementing the LayerToCutTransformer interface, responsible for transforming
 * a hierarchical graph layer into a corresponding graph cut within the context of the IntelliJ IDEA Plugin.
 */
object IJLayerToCutTransformer :
LayerToCutTransformer<CondensedInstanceLevelNode, CondensedInstanceLevelEdge, CondensedInstanceLevelGraph, IJDDContext> {
    private var currentLayer: Set<CondensedInstanceLevelNode> = emptySet()

    /**
     * Transforms the given layer by modifying the graph context and removing dependencies corresponding to the deleted nodes.
     * The current implementation of the transformer tracks the removed from the layer nodes:
     * * [GraphLayerHierarchyProducer] on each produced layer, it is provided via [IJLayerToCutTransformer.setCurrentLayer]
     * * Then, on the transformation the chosen by [DDAlgorithm] layer is transformed to a complement to the whole layer
     * * The complement is the set of deleted from the layer vertices.
     * * Then all the vertices that are dependent on these deleted vertices are selected via [DeletedDependenciesCollector] to the final cut.
     *     * Some vertices could be already deleted from the graph. In this case, the [DeletedDependenciesCollector] will not traverse them, which means that they will not be included in the final cut.
     * * The complement of the deleted vertices cut is the final cut that is returned to the property checker.
     * @param layer A list of `CondensedInstanceLevelNode` representing the layer to be transformed.
     * @param context An `IJDDContext` representing the context for the transformation, including the graph to operate on.
     * @return A `GraphCut<CondensedInstanceLevelNode>` object containing the vertices that remain after removing the dependencies of the deleted nodes.
     */
    override suspend fun transform(
        layer: List<CondensedInstanceLevelNode>,
        context: IJDDContext
    ): GraphCut<CondensedInstanceLevelNode> {
        val layerToDelete = currentLayer - layer.toSet()
        val graph =
            requireNotNull(context.graph) { "To use graph algorithms graph should be set and condensed graph should exists" }
        val collector = DeletedDependenciesCollector<PsiStubDDItem, PsiIJEdge>(layerToDelete)
        val deletedCut = collector.visitGraph(graph)
        return GraphCut((graph.vertices.toSet() - deletedCut).toList())
    }

    override suspend fun setCurrentLayer(layer: List<CondensedInstanceLevelNode>) {
        currentLayer = layer.toSet()
    }
}

private class DeletedDependenciesCollector<V : DDItem, E : GraphEdge<V>>(private val deletedElements: Set<CondensedVertex<V, E>>) :
    DepthFirstGraphWalkerVoid<
            CondensedVertex<V, E>,
            CondensedEdge<V, E>,
            CondensedGraph<V, E>,
            Set<CondensedVertex<V, E>>>() {
    private val collectedElements = mutableSetOf<CondensedVertex<V, E>>()
    override suspend fun onUnvisitedNode(graph: CondensedGraph<V, E>, node: CondensedVertex<V, E>) {
        if (node in deletedElements) {
            collectedElements.add(node)
        } else {
            super.onUnvisitedNode(graph, node)
        }
    }

    override suspend fun onPassedEdge(graph: CondensedGraph<V, E>, edge: CondensedEdge<V, E>) {
        if (edge.to in collectedElements) {
            collectedElements.add(edge.from)
        }
    }

    override suspend fun onComplete(graph: CondensedGraph<V, E>) = collectedElements
}