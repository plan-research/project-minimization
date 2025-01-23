package org.plan.research.minimization.core.algorithm.dd.impl.graph

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HDDLevel
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDDGenerator
import org.plan.research.minimization.core.model.*

import arrow.core.filterOption
import arrow.core.raise.option
import org.jgrapht.Graph
import org.jgrapht.graph.AsSubgraph

class GraphLayerHierarchyGenerator<M : Monad, T : DDItem, E>(
    private val graphPropertyTesterWithGraph: GraphPropertyTester<M, T>,
    var graph: Graph<T, E>,
) : HierarchicalDDGenerator<WithProgressMonadT<M>, M, T> {
    private val layerToCutTransformer = LayerToCutTransformer<T>()
    private val tester = LayerToCut()
    private val originalGraph = graph
    private val depthCounter = DepthCounter.create(graph)
    private val inactiveCount = mutableMapOf<T, Int>()

    context(WithProgressMonadT<M>)
    override suspend fun generateFirstLevel() = option {
        val sinks = graph.vertexSet().filter {
            graph.outDegreeOf(it) == 0
        }
        ensure(sinks.isNotEmpty())

        reportProgress(sinks)
        HDDLevel(sinks, tester)
    }

    context(WithProgressMonadT<M>)
    override suspend fun generateNextLevel(minimizationResult: DDAlgorithmResult<T>) = option {
        val nextItems = minimizationResult.retained
            .asSequence()
            .getNextCandidates()
            .updateCandidates()
            .filterCandidates()
            .toList()

        ensure(nextItems.isNotEmpty())

        reportProgress(nextItems)
        HDDLevel(nextItems, tester)
    }

    context(WithProgressMonadT<M>)
    private fun reportProgress(level: List<T>) {
        val minDepth = level
            .map { depthCounter.getDepthOf(it) }
            .filterOption()
            .min()

        nextStep(minDepth, depthCounter.maxDepth)
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
