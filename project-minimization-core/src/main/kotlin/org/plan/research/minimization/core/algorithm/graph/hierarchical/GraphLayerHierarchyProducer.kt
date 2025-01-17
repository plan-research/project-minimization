package org.plan.research.minimization.core.algorithm.graph.hierarchical

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HDDLevel
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDDGenerator
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.Monad
import org.plan.research.minimization.core.model.MonadT
import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.core.model.PropertyTesterWithGraph
import org.plan.research.minimization.core.model.graph.Graph
import org.plan.research.minimization.core.model.graph.GraphEdge

import arrow.core.Option
import arrow.core.raise.option

abstract class GraphLayerHierarchyProducer<V, E, G, M, M2>(
    private val layerToCutTransformer: LayerToCutTransformer<V, E, G, M2>,
    private val graphPropertyTesterWithGraph: PropertyTesterWithGraph<M2, V>,
) : HierarchicalDDGenerator<M, M2, V> where V : DDItem, E : GraphEdge<V>, G : Graph<V, E, G>, M : MonadT<M2>, M2 : Monad {
    private val tester = LayerToCut()
    context(M)
    protected abstract suspend fun generateFirstGraphLayer(): Option<GraphLayer>

    context(M)
    protected abstract suspend fun generateNextGraphLayer(minimizationResult: DDAlgorithmResult<V>): Option<GraphLayer>

    context(M)
    final override suspend fun generateFirstLevel(): Option<HDDLevel<M2, V>> = option {
        val layer = generateFirstGraphLayer().bind()
        HDDLevel(items = layer.layer, propertyTester = tester)
    }
    context(M)
    final override suspend fun generateNextLevel(minimizationResult: DDAlgorithmResult<V>) = option {
        val layer = generateNextGraphLayer(minimizationResult).bind()
        HDDLevel(items = layer.layer, propertyTester = tester)
    }

    protected inner class GraphLayer(val layer: List<V>)
    private inner class LayerToCut : PropertyTester<M2, V> {
        context(M2)
        override suspend fun test(
            survivedItems: List<V>,
            deletedItems: List<V>,
        ): PropertyTestResult {
            val (survivedCut, deletedCut) = layerToCutTransformer.transform(survivedItems, deletedItems)
            return graphPropertyTesterWithGraph.test(survivedCut, deletedCut)
        }
    }
}
