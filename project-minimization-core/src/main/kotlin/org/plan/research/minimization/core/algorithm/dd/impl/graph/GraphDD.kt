package org.plan.research.minimization.core.algorithm.dd.impl.graph

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.core.algorithm.dd.DDGraphAlgorithm
import org.plan.research.minimization.core.algorithm.dd.DDGraphAlgorithmResult
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDD
import org.plan.research.minimization.core.algorithm.dd.withZeroTesting
import org.plan.research.minimization.core.model.*

import org.jgrapht.Graph

class GraphDD<T : DDItem>(
    underlyingAlgorithm: DDAlgorithm<T>,
    private val graphLayerMonadTProvider: GraphLayerMonadTProvider,
) : DDGraphAlgorithm<T> {
    private val hdd = HierarchicalDD(underlyingAlgorithm.withZeroTesting())

    context(M)
    override suspend fun <M : Monad, E> minimize(
        graph: Graph<T, E>,
        propertyTester: GraphPropertyTester<M, T>,
    ): DDGraphAlgorithmResult<T> {
        val graphLayerHierarchyGenerator = GraphLayerHierarchyGenerator(propertyTester, graph)

        graphLayerMonadTProvider.runUnderProgress {
            hdd.minimize(graphLayerHierarchyGenerator)
        }

        val retained = graphLayerHierarchyGenerator.graph.vertexSet()
        val deleted = graph.vertexSet() - retained

        return DDGraphAlgorithmResult(retained, deleted)
    }

    interface GraphLayerMonadTProvider {
        context(M)
        suspend fun <M : Monad> runUnderProgress(block: WithProgressMonadFAsync<M, Unit>)
    }
}
