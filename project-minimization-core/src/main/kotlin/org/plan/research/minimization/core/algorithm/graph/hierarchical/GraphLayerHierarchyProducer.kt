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
import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.core.model.graph.GraphWithAdjacencyList

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
 */
abstract class GraphLayerHierarchyProducer<V, E, G, M, M2>(
    private val graphPropertyTesterWithGraph: PropertyTesterWithGraph<M2, V>,
) : HierarchicalDDGenerator<M, M2, V> where V : DDItem, E : GraphEdge<V>, G : GraphWithAdjacencyList<V, E, G>, M : MonadT<M2>, M2 : Monad {
    private val layerToCutTransformer = LayerToCutTransformer<V, E, G>()
    private val tester = LayerToCut()

    /**
     * An extension of [HierarchicalDDGenerator.generateFirstLevel] to produce an initial graph layer.
     */
    context(M)
    protected abstract suspend fun generateFirstGraphLayer(): Option<GraphLayer>

    /**
     * An extension
     * of [HierarchicalDDGenerator.generateNextLevel] to produce a next graph layer using [minimizationResult].
     *
     * **Invariant: the graph will be changed on the focusing stage, but not during the producing the new layer**
     *
     * @param minimizationResult
     */
    context(M)
    protected abstract suspend fun generateNextGraphLayer(minimizationResult: DDAlgorithmResult<V>): Option<GraphLayer>

    context(M)
    final override suspend fun generateFirstLevel() = option {
        val layer = generateFirstGraphLayer().bind()
        HDDLevel(items = layer.layer, propertyTester = tester)
    }
    context(M)
    final override suspend fun generateNextLevel(minimizationResult: DDAlgorithmResult<V>) = option {
        val layer = generateNextGraphLayer(minimizationResult).bind()
        HDDLevel(items = layer.layer, propertyTester = tester)
    }

    context(M2)
    protected abstract fun graph(): G

    protected inner class GraphLayer(val layer: List<V>)
    private inner class LayerToCut : PropertyTester<M2, V> {
        context(M2)
        override suspend fun test(items: List<V>, deletedItems: List<V>): PropertyTestResult {
            val graph = graph()
            val deletedCut = layerToCutTransformer.transform(graph, deletedItems)
            val retainedCut = graph - deletedCut
            return graphPropertyTesterWithGraph.test(retainedCut, deletedCut)
        }
    }
}
