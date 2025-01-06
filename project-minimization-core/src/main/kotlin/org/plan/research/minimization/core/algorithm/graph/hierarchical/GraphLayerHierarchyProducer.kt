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

abstract class GraphLayerHierarchyProducer<V, E, G, C>(
    private val layerToCutTransformer: LayerToCutTransformer<V, E, G, C>,
    private val graphPropertyTesterWithGraph: PropertyTesterWithGraph<C, V, E, G>,
) : HierarchicalDDGenerator<C, V> where V : DDItem, E : GraphEdge<V>, G : Graph<V, E, G>, C : DDContextWithLevel<C> {
    private val tester = LayerToCut()
    protected abstract fun generateFirstGraphLayer(context: C): Option<GraphLayer>
    protected abstract fun generateNextGraphLayer(minimizationResult: DDAlgorithmResult<C, V>): Option<GraphLayer>

    final override suspend fun generateFirstLevel(context: C) = option {
        val layer = generateFirstGraphLayer(context).bind()
        HDDLevel(context = layer.context, items = layer.layer, propertyTester = tester)
    }

    final override suspend fun generateNextLevel(minimizationResult: DDAlgorithmResult<C, V>) = option {
        val layer = generateNextGraphLayer(minimizationResult).bind()
        HDDLevel(context = layer.context, items = layer.layer, propertyTester = tester)
    }

    protected inner class GraphLayer(val layer: List<V>, val context: C)
    private inner class LayerToCut : PropertyTester<C, V> {
        override suspend fun test(
            context: C,
            items: List<V>,
        ): PropertyTestResult<C> {
            val cut = layerToCutTransformer.transform(items, context)
            return graphPropertyTesterWithGraph.test(context, cut)
        }
    }
}
