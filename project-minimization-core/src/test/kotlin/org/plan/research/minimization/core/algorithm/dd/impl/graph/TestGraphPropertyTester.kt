package org.plan.research.minimization.core.algorithm.dd.impl.graph

import arrow.core.raise.OptionRaise
import arrow.core.raise.option
import org.jgrapht.Graph
import org.jgrapht.graph.AsSubgraph
import org.jgrapht.graph.DefaultEdge
import org.plan.research.minimization.core.model.*

abstract class TestGraphPropertyTester(
    val originalGraph: TestGraph,
) : GraphPropertyTester<EmptyMonad, TestNode> {
    private var currentGraph: Graph<TestNode, DefaultEdge> = originalGraph

    abstract fun OptionRaise.testImpl(
        retainedCut: GraphCut<TestNode>,
        deletedCut: GraphCut<TestNode>
    )

    context(EmptyMonad)
    final override suspend fun test(
        retainedCut: GraphCut<TestNode>,
        deletedCut: GraphCut<TestNode>,
    ): PropertyTestResult {
        checkCuts(currentGraph, retainedCut, deletedCut)

        return option { testImpl(retainedCut, deletedCut) }
            .onSome { currentGraph = AsSubgraph(originalGraph, retainedCut, currentGraph.edgeSet()) }
            .toEither { PropertyTesterError.NoProperty }
    }
}
