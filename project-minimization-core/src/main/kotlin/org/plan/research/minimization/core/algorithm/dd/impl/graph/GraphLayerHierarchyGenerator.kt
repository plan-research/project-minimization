package org.plan.research.minimization.core.algorithm.dd.impl.graph

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HDDLevel
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDDGenerator
import org.plan.research.minimization.core.model.*

import arrow.core.raise.option
import org.jgrapht.Graph
import org.jgrapht.graph.AsSubgraph

abstract class GraphLayerMonadT<M : Monad, T : DDItem>(monad: M) : MonadT<M>(monad) {
    abstract fun onNextLevel(level: HDDLevel<M, T>)
}

class GraphLayerHierarchyGenerator<M : Monad, T : DDItem, E>(
    private val graphPropertyTesterWithGraph: GraphPropertyTester<M, T>,
    var graph: Graph<T, E>,
) : HierarchicalDDGenerator<GraphLayerMonadT<M, T>, M, T> {
    private val layerToCutTransformer = LayerToCutTransformer<T>()
    private val tester = LayerToCut()
    private val originalGraph = graph
    private val inactiveCount = mutableMapOf<T, Int>()

    context(GraphLayerMonadT<M, T>)
    override suspend fun generateFirstLevel() = option {
        val sinks = graph.vertexSet().filter {
            graph.outDegreeOf(it) == 0
        }
        ensure(sinks.isNotEmpty())
        HDDLevel(sinks, tester).also { onNextLevel(it) }
    }

    context(GraphLayerMonadT<M, T>)
    override suspend fun generateNextLevel(minimizationResult: DDAlgorithmResult<T>) = option {
        val nextItems = minimizationResult.retained
            .asSequence()
            .getNextCandidates()
            .updateCandidates()
            .filterCandidates()
            .toList()

        ensure(nextItems.isNotEmpty())

        HDDLevel(nextItems, tester).also { onNextLevel(it) }
    }

    private fun Sequence<T>.getNextCandidates(): Sequence<T> =
        flatMap { graph.incomingEdgesOf(it) }
            .map { graph.getEdgeSource(it) }

    private fun Sequence<T>.updateCandidates(): Sequence<T> =
        onEach { inactiveCount.merge(it, 1, Int::plus) }

    private fun Sequence<T>.filterCandidates(): Sequence<T> =
        distinct().filter { inactiveCount[it] == graph.outDegreeOf(it) }

    private inner class LayerToCut : PropertyTester<M, T> {
        context(M)
        override suspend fun test(
            retainedItems: List<T>,
            deletedItems: List<T>,
        ): PropertyTestResult {
            val (retainedCut, deletedCut) = layerToCutTransformer.transform(graph, deletedItems)
            return graphPropertyTesterWithGraph.test(retainedCut, deletedCut).onRight {
                graph = AsSubgraph(originalGraph, retainedCut, graph.edgeSet())
            }
        }
    }
}
