package org.plan.research.minimization.core.algorithm.dd.impl.graph.domain

import arrow.core.raise.either
import arrow.core.raise.ensure
import org.jgrapht.graph.DefaultEdge
import org.plan.research.minimization.core.algorithm.dd.impl.graph.TestGraph
import org.plan.research.minimization.core.algorithm.dd.impl.graph.TestGraphPropertyTester
import org.plan.research.minimization.core.algorithm.dd.impl.graph.TestNode
import org.plan.research.minimization.core.model.EmptyMonad
import org.plan.research.minimization.core.model.GraphCut
import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.core.model.PropertyTesterError

class SingleGraphPropertyTester(
    val targetNode: TestNode,
    val originalGraph: TestGraph,
) : TestGraphPropertyTester {
    context(EmptyMonad)
    override suspend fun test(
        retainedCut: GraphCut<TestNode, DefaultEdge>,
        deletedCut: GraphCut<TestNode, DefaultEdge>,
    ): PropertyTestResult = either {
        ensure(targetNode in retainedCut.vertexSet()) {
            PropertyTesterError.NoProperty
        }
    }
}
