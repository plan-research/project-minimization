package org.plan.research.minimization.plugin.hierarchy.graph

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.algorithm.graph.hierarchical.GraphLayerHierarchyProducer
import org.plan.research.minimization.core.model.lift
import org.plan.research.minimization.plugin.model.IJGraphPropertyTester
import org.plan.research.minimization.plugin.model.IJInstanceLevelLayerHierarchyGenerator
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.context.WithInstanceLevelGraphContext
import org.plan.research.minimization.plugin.model.monad.SnapshotMonad
import org.plan.research.minimization.plugin.model.monad.SnapshotWithProgressMonad
import org.plan.research.minimization.plugin.psi.graph.CondensedInstanceLevelEdge
import org.plan.research.minimization.plugin.psi.graph.CondensedInstanceLevelGraph
import org.plan.research.minimization.plugin.psi.graph.CondensedInstanceLevelNode

import arrow.core.filterOption
import arrow.core.getOrElse
import arrow.core.raise.option
import mu.KotlinLogging

private typealias AlgorithmResult = DDAlgorithmResult<CondensedInstanceLevelNode>

/**
 * A specific implementation of [GraphLayerHierarchyProducer] for producing hierarchical layers
 * from a condensed instance-level graph.
 *
 * The implementation of that class is based on some like of reversed breath-first search.
 * Any element of the graph could be in one of the following states:
 * * Not visited yet — that vertex has not been processed by the hierarchy producer yet
 * * Active — that vertex is now chosen to be in the next layer
 * * Deleted — that vertex from the current layer is chosen to be deleted by DD algorithm. That vertex is deleted from the graph.
 * * Selected to left nodes — that vertex from the current layer is determined as important for graph
 * * Inactive — This vertex is adjacent with an active node. That vertex is propagated from some active layer, but not all out-coming edges are taken into account
 */
class InstanceLevelLayerHierarchyProducer<C : WithInstanceLevelGraphContext<C>>(
    graphPropertyTester: IJGraphPropertyTester<C, CondensedInstanceLevelNode>,
) : IJInstanceLevelLayerHierarchyGenerator<C, CondensedInstanceLevelNode, CondensedInstanceLevelEdge, CondensedInstanceLevelGraph>(
    graphPropertyTester,
) {
    private val logger = KotlinLogging.logger { }
    private val inactiveElements: MutableMap<CondensedInstanceLevelNode, Int> = mutableMapOf()
    private lateinit var depthCounter: DepthCounter<CondensedInstanceLevelNode>
    context(SnapshotWithProgressMonad<C>)
    override suspend fun generateFirstGraphLayer() = option {
        lift {
            depthCounter = DepthCounter.create(context.graph)
            val layer = context.graph.sinks
            ensure(layer.isNotEmpty())
            logger.debug { "[INSTANCE_GRAPH] produced ${layer.size} vertices on the first layer" }
            GraphLayer(
                layer = layer,
            ).also { it.reportProgress() }
        }
    }

    /**
     * Generates the next graph layer in the hierarchy based on the provided minimization result.
     *
     * The deleted on the current level vertices are processed automatically: they were removed from the graph.
     *
     * * Then from the left active vertices propagates the state to the predecessors.
     * * That produces a list of the inactive elements that were updated during this layer.
     * * From the list the next active elements are retrieved by checking if all out-coming edges have been processed.
     * * These new active elements are the only possible candidates, since rest vertices hasn't been updated on that level.
     *   Thus, we know that there is some unprocessed edge out of the vertex.
     */
    context(SnapshotWithProgressMonad<C>)
    override suspend fun generateNextGraphLayer(minimizationResult: AlgorithmResult) = option {
        minimizationResult.propagateActive()
            .bind()
            .let { (context, nextInactiveElements) -> context.produceNextLevel(nextInactiveElements) }
            .bind()
            .also { it.reportProgress() }
    }

    /**
     * This function propagates to inactive elements information about completed active elements
     */
    context(SnapshotWithProgressMonad<C>)
    private fun AlgorithmResult.propagateActive() = option {
        lift {
            val graph = context.graph
            val nextInactiveElements =
                this@propagateActive.retained.flatMap { graph.edgesTo(it).getOrElse { emptyList() } }
                    .map { it.from }
                    .onEach { inactiveElements.merge(it, 1, Int::plus) }
            logger.debug { "After the stage there are  ${nextInactiveElements.size} (in-)active elements" }
            context to nextInactiveElements.distinct()
        }
    }

    /**
     * The function that processes all processed inactive elements and produces the new layer by choosing the new active elements
     */
    context(SnapshotWithProgressMonad<C>)
    private fun IJDDContext.produceNextLevel(nextInactiveElements: List<CondensedInstanceLevelNode>) = option {
        lift {
            val graph = context.graph
            val nextElements =
                nextInactiveElements.filter { vertex -> graph.outDegreeOf(vertex) == inactiveElements[vertex] }
            ensure(nextElements.isNotEmpty())
            logger.debug { "On the next stage there are ${nextElements.size} active elements" }
            GraphLayer(
                layer = nextElements,
            )
        }
    }

    context(SnapshotMonad<C>)
    override fun graph(): CondensedInstanceLevelGraph = context.graph

    context(SnapshotWithProgressMonad<C>)
    private fun GraphLayer.reportProgress() {
        val layerPercent = SnapshotWithProgressMonad.MAXIMUM_FRACTION - layer.asSequence()
            .map { depthCounter.getPercent(it) }
            .filterOption()
            .min()
        nextStep(layerPercent.coerceAtLeast(1))
    }
}
