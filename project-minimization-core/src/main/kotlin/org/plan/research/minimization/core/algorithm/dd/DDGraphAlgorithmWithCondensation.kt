package org.plan.research.minimization.core.algorithm.dd

import org.plan.research.minimization.core.model.*

import org.jgrapht.Graph
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.SimpleDirectedGraph
import org.jgrapht.util.SupplierUtil

typealias CondensedGraph<T> = Graph<CondensedVertex<T>, DefaultEdge>

data class CondensedVertex<T : DDItem>(val items: Set<T>) : DDItem

@Suppress("TYPE_ALIAS")
class DDGraphAlgorithmWithCondensation(
    private val underlyingAlgorithm: DDGraphAlgorithm,
) : DDGraphAlgorithm {
    private fun <T : DDItem, E> condense(graph: Graph<T, E>): CondensedGraph<T> {
        val strongConnectivityAlgorithm = KosarajuStrongConnectivityInspector(graph)
        val components = strongConnectivityAlgorithm.stronglyConnectedComponents
            .map { CondensedVertex(it.vertexSet()) }

        val condensedGraph = SimpleDirectedGraph.createBuilder<CondensedVertex<T>, DefaultEdge>(
            SupplierUtil.createDefaultEdgeSupplier(),
        )

        val vertex2Component = HashMap<T, Int>(graph.vertexSet().size)

        components.forEachIndexed { index, component ->
            condensedGraph.addVertex(component)
            component.items.forEach { vertex2Component[it] = index }
        }

        graph.edgeSet().forEach { edge ->
            val source = graph.getEdgeSource(edge)
            val target = graph.getEdgeTarget(edge)
            val sourceComponentIndex = vertex2Component[source]!!
            val targetComponentIndex = vertex2Component[target]!!
            if (sourceComponentIndex != targetComponentIndex) {
                condensedGraph.addEdge(components[sourceComponentIndex], components[targetComponentIndex])
            }
        }

        return condensedGraph.build()
    }

    context(M)
    override suspend fun <M : Monad, T : DDItem, E> minimize(
        graph: Graph<T, E>,
        propertyTester: GraphPropertyTester<M, T>,
    ): DDGraphAlgorithmResult<T> {
        val condensedGraph = condense(graph)
        val propertyTesterAdapter = GraphPropertyTesterAdapter(propertyTester)

        val (retained, deleted) = underlyingAlgorithm.minimize(condensedGraph, propertyTesterAdapter)

        return DDGraphAlgorithmResult(retained.flatten(), deleted.flatten())
    }

    private fun <T : DDItem> GraphCut<CondensedVertex<T>>.flatten(
    ): GraphCut<T> = fold(mutableSetOf()) { acc, vertex ->
        acc.apply {
            addAll(vertex.items)
        }
    }

    private inner class GraphPropertyTesterAdapter<M : Monad, T : DDItem>(
        val propertyTester: GraphPropertyTester<M, T>,
    ) : GraphPropertyTester<M, CondensedVertex<T>> {
        context(M)
        override suspend fun test(
            retainedCut: GraphCut<CondensedVertex<T>>,
            deletedCut: GraphCut<CondensedVertex<T>>,
        ): PropertyTestResult = propertyTester.test(retainedCut.flatten(), deletedCut.flatten())
    }
}

fun DDGraphAlgorithm.withCondensation(): DDGraphAlgorithm =
    DDGraphAlgorithmWithCondensation(this)
