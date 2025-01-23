package org.plan.research.minimization.core.algorithm.dd.impl.graph

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.core.algorithm.dd.DDGraphAlgorithm
import org.plan.research.minimization.core.algorithm.dd.DDGraphAlgorithmResult
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDD
import org.plan.research.minimization.core.algorithm.dd.withZeroTesting
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.GraphPropertyTester
import org.plan.research.minimization.core.model.Monad

import org.jgrapht.Graph

class GraphDD(
    underlyingAlgorithm: DDAlgorithm,
    private val graphLayerMonadTProvider: GraphLayerMonadTProvider,
) : DDGraphAlgorithm {
    private val hdd = HierarchicalDD(underlyingAlgorithm.withZeroTesting())

    context(M)
    override suspend fun <M : Monad, T : DDItem, E> minimize(
        graph: Graph<T, E>,
        propertyTester: GraphPropertyTester<M, T>,
    ): DDGraphAlgorithmResult<T> {
        val graphLayerHierarchyGenerator = GraphLayerHierarchyGenerator(propertyTester, graph)

        graphLayerMonadTProvider.provide<M, T>().run {
            hdd.minimize(graphLayerHierarchyGenerator)
        }

        val retained = graphLayerHierarchyGenerator.graph.vertexSet()
        val deleted = graph.vertexSet() - retained

        return DDGraphAlgorithmResult(retained, deleted)
    }

    interface GraphLayerMonadTProvider {
        context(M)
        fun <M : Monad, T : DDItem> provide(): GraphLayerMonadT<M, T>
    }
}
