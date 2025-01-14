package org.plan.research.minimization.core.algorithm.graph.hierarchical

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HDDLevel
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDDGenerator
import org.plan.research.minimization.core.model.DDContextWithLevel
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.core.model.PropertyTesterWithGraph
import org.plan.research.minimization.core.model.graph.Graph
import org.plan.research.minimization.core.model.graph.GraphEdge

import arrow.core.Option
import arrow.core.raise.option

/**
 * A base abstract class responsible for generating hierarchical layers from a graph-based structure.
 * Extends the behavior of a hierarchical delta debugging generator, specializing it
 * for graph-like data with associated properties and transformations.
 *
 *
 * @param V The type of vertices.
 * @param E The type of edges.
 * @param G The graph type.
 * @param C The contextual information type.
 */
abstract class GraphLayerHierarchyProducer<V, E, G, C>(
    private val layerToCutTransformer: LayerToCutTransformer<V, E, G, C>,
    private val graphPropertyTester: PropertyTesterWithGraph<C, V, E, G>,
) : HierarchicalDDGenerator<C, V> where V : DDItem, E : GraphEdge<V>, G : Graph<V, E, G>, C : DDContextWithLevel<C> {
    private val tester = LayerToCut()

    /**
     * An extension of [HierarchicalDDGenerator.generateFirstLevel] to produce an initial graph layer.
     *
     * @param context
     */
    protected abstract suspend fun generateFirstGraphLayer(context: C): Option<GraphLayer>

    /**
     * An extension
     * of [HierarchicalDDGenerator.generateNextLevel] to produce a next graph layer using [minimizationResult].
     *
     * **Invariant: the graph will be changed on the focusing stage, but not during the producing the new layer**
     *
     * @param minimizationResult
     */
    protected abstract suspend fun generateNextGraphLayer(minimizationResult: DDAlgorithmResult<C, V>): Option<GraphLayer>
    final override suspend fun generateFirstLevel(context: C) = option {
        val layer = generateFirstGraphLayer(context).bind()
        layerToCutTransformer.setCurrentLayer(layer.layer)
        HDDLevel(context = layer.context, items = layer.layer, propertyTester = tester)
    }

    final override suspend fun generateNextLevel(minimizationResult: DDAlgorithmResult<C, V>) = option {
        val layer = generateNextGraphLayer(minimizationResult).bind()
        layerToCutTransformer.setCurrentLayer(layer.layer)
        HDDLevel(context = layer.context, items = layer.layer, propertyTester = tester)
    }

    protected inner class GraphLayer(val layer: List<V>, val context: C)

    /**
     * A private class responsible for testing properties on the produced layers.
     * It uses [layerToCutTransformer] to transform layer to cut and pass it to [graphPropertyTester]
     */
    private inner class LayerToCut : PropertyTester<C, V> {
        override suspend fun test(
            context: C,
            items: List<V>,
        ): PropertyTestResult<C> {
            val cut = layerToCutTransformer.transform(items, context)
            return graphPropertyTester.test(context, cut)
        }
    }
}
