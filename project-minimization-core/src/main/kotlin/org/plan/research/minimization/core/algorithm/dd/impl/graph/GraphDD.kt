package org.plan.research.minimization.core.algorithm.dd.impl.graph

import org.jgrapht.Graph
import org.jgrapht.graph.AsSubgraph
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.core.algorithm.dd.DDGraphAlgorithm
import org.plan.research.minimization.core.algorithm.dd.DDGraphAlgorithmResult
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDD
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.GraphPropertyTester
import org.plan.research.minimization.core.model.Monad

class GraphDD(
    underlyingAlgorithm: DDAlgorithm,
) : DDGraphAlgorithm {

    private val hdd = HierarchicalDD(underlyingAlgorithm)

    context(GraphLayerMonadT<M, T>)
    override suspend fun <M : Monad, T : DDItem, E> minimize(
        graph: Graph<T, E>,
        propertyTester: GraphPropertyTester<M, T, E>,
    ): DDGraphAlgorithmResult<T, E> {
        val graphLayerHierarchyGenerator = GraphLayerHierarchyGenerator(propertyTester, graph)
        hdd.minimize(graphLayerHierarchyGenerator)

        val retained = graphLayerHierarchyGenerator.graph.asSubgraph()
        val deleted = AsSubgraph(graph, graph.vertexSet() - retained.vertexSet())

        return DDGraphAlgorithmResult(retained, deleted)
    }

    private fun <V, E> Graph<V, E>.asSubgraph() = (this as? AsSubgraph) ?: AsSubgraph(this)
}
