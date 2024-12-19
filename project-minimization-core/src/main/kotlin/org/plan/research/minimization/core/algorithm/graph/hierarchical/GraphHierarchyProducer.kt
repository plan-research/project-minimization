package org.plan.research.minimization.core.algorithm.graph.hierarchical

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HDDLevel
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDDGenerator
import org.plan.research.minimization.core.algorithm.graph.GraphContext
import org.plan.research.minimization.core.algorithm.graph.condensation.CondensedGraph
import org.plan.research.minimization.core.algorithm.graph.condensation.CondensedVertex
import org.plan.research.minimization.core.algorithm.graph.condensation.StrongConnectivityCondensation
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.core.model.graph.GraphWithAdjacencyList

import arrow.core.getOrElse
import arrow.core.raise.ensureNotNull
import arrow.core.raise.option

import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentMapOf

private typealias GraphHierarchyPropertyTester<V, E, G, C> = PropertyTester<GraphHierarchicalDDContext<V, E, G, C>, CondensedVertex<V>>
private typealias DdGraphResult<V, E, G, C> = DDAlgorithmResult<GraphHierarchicalDDContext<V, E, G, C>, CondensedVertex<V>>

internal class GraphHierarchyProducer<V, E, G, C>(val propertyTester: GraphHierarchyPropertyTester<V, E, G, C>) :
    HierarchicalDDGenerator<GraphHierarchicalDDContext<V, E, G, C>, CondensedVertex<V>>
where V : DDItem,
E : GraphEdge<V>,
G : GraphWithAdjacencyList<V, E>,
C : GraphContext<V, E, G> {
    override suspend fun generateFirstLevel(context: GraphHierarchicalDDContext<V, E, G, C>) = option {
        val condensedVertexSet = StrongConnectivityCondensation.compressGraph(context.graph)
        val compressedGraph = CondensedGraph.from(condensedVertexSet)
        val sinks = compressedGraph
            .sinks

        ensure(sinks.isNotEmpty())
        HDDLevel(
            context = context.copy(
                currentLevel = sinks,
                inactiveElements = persistentMapOf(),
                condensedGraph = compressedGraph,
            ),
            propertyTester = propertyTester,
            items = sinks,
        )
    }

    override suspend fun generateNextLevel(minimizationResult: DdGraphResult<V, E, G, C>) =
        option {
            val deletedPropagated = minimizationResult.propagateDeleted()
            val (propagatedContext, nextInactiveElements) = deletedPropagated.propagateActive().bind()
            propagatedContext
                .produceNextLevel(nextInactiveElements)
                .bind()
        }

    private suspend fun DdGraphResult<V, E, G, C>.propagateDeleted(): DdGraphResult<V, E, G, C> {
        val graph = requireNotNull(context.condensedGraph)

        val currentLevel = requireNotNull(context.currentLevel)
        require(currentLevel.all { it is CondensedVertex<*> })
        val deletedItems = (currentLevel as List<CondensedVertex<V>>).toSet() - items.toSet()
        val deletedDependenciesCollector = DeletedDependenciesCollector<V, E>(deletedItems)
        val deletedWithDependencies = deletedDependenciesCollector.visitGraph(graph)
        val newContext = context.copy(
            inactiveElements = context.inactiveElements.mutate { map -> deletedWithDependencies.forEach(map::remove) },
            condensedGraph = graph.withoutNodes(deletedWithDependencies),
        )
        return DdGraphResult<V, E, G, C>(
            newContext,
            items,
        )
    }

    private fun DdGraphResult<V, E, G, C>.propagateActive() = option {
        val graph = requireNotNull(context.condensedGraph)
        val nextInactiveElements = items
            .flatMap { graph.edgesTo(it).getOrElse { emptyList() } }
            .map { it.from }
        context.copy(
            inactiveElements = context.inactiveElements.mutate { map ->
                nextInactiveElements.forEach {
                    map.merge(it, 1, Int::plus)
                }
            },
        ) to nextInactiveElements.distinct()
    }

    private fun GraphHierarchicalDDContext<V, E, G, C>.produceNextLevel(nextInactiveElements: List<CondensedVertex<V>>) =
        option {
            val graph = ensureNotNull(condensedGraph)
            val nextElements = nextInactiveElements
                .filter { vertex -> graph.outDegreeOf(vertex) == inactiveElements[vertex] }
            ensure(nextElements.isNotEmpty())
            val newContext = copy(
                currentLevel = nextElements,
                inactiveElements = inactiveElements.mutate { it -= nextElements },
            )
            HDDLevel(
                context = newContext,
                items = nextElements,
                propertyTester = propertyTester,
            )
        }
}
