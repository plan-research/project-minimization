package org.plan.research.minimization.core.algorithm.dd.impl.graph

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HDDLevel
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDDGenerator
import org.plan.research.minimization.core.model.*

import arrow.core.filterOption
import arrow.core.raise.option
import org.jgrapht.Graph
import org.jgrapht.graph.AsSubgraph

/**
 * An implementation for producing hierarchical layers from a graph for the [GraphDD] algorithm.
 *
 * The implementation of that class is based on some like of reversed breath-first search.
 * Any element of the graph could be in one of the following states:
 * * Not visited yet — that vertex has not been processed by the hierarchy producer yet
 * * Active — that vertex is now chosen to be in the next layer
 * * Deleted — that vertex from the current layer is chosen to be deleted by DD algorithm. That vertex is deleted from the graph.
 * * Selected to left nodes — that vertex from the current layer is determined as important for graph
 * * Inactive — This vertex is adjacent with an active node. That vertex is propagated from some active layer, but not all out-coming edges are taken into account
 */
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

    /**
     * Generates the next graph layer in the hierarchy based on the provided minimization result.
     *
     * The deleted on the current level vertices are processed automatically: they were removed from the graph.
     *
     * * Then from the left active vertices propagates the state to the predecessors.
     * * That produces a list of the inactive elements that were updated during this layer.
     * * From the list the next active elements are retrieved by checking if all out-coming edges have been processed.
     * * These new active elements are the only possible candidates, since rest vertices haven't been updated on that level.
     *   Thus, we know that there is some unprocessed edge out of the vertex.
     */
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
