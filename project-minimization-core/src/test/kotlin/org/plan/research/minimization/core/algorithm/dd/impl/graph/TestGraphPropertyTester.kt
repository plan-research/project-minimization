package org.plan.research.minimization.core.algorithm.dd.impl.graph

import arrow.core.raise.OptionRaise
import arrow.core.raise.option
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultEdge
import org.plan.research.minimization.core.model.*

abstract class TestGraphPropertyTester(
    val originalGraph: TestGraph,
) : GraphPropertyTester<EmptyMonad, TestNode, DefaultEdge> {
    private var currentGraph: Graph<TestNode, DefaultEdge> = originalGraph

    abstract fun OptionRaise.testImpl(
        retainedCut: GraphCut<TestNode, DefaultEdge>,
        deletedCut: GraphCut<TestNode, DefaultEdge>
    )

    context(EmptyMonad)
    final override suspend fun test(
        retainedCut: GraphCut<TestNode, DefaultEdge>,
        deletedCut: GraphCut<TestNode, DefaultEdge>,
    ): PropertyTestResult {
        checkCuts(currentGraph, retainedCut, deletedCut)

        return option { testImpl(retainedCut, deletedCut) }
            .onSome { currentGraph = retainedCut }
            .toEither { PropertyTesterError.NoProperty }
    }
}