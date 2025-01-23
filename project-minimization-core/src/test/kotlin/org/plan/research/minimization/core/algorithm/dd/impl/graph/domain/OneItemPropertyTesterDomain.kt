package org.plan.research.minimization.core.algorithm.dd.impl.graph.domain

import arrow.core.raise.OptionRaise
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.ForAll
import net.jqwik.api.Provide
import net.jqwik.api.domains.DomainContextBase
import org.plan.research.minimization.core.algorithm.dd.impl.graph.TestGraph
import org.plan.research.minimization.core.algorithm.dd.impl.graph.TestGraphPropertyTester
import org.plan.research.minimization.core.algorithm.dd.impl.graph.TestNode
import org.plan.research.minimization.core.model.GraphCut

class OneItemPropertyTesterDomain : DomainContextBase() {
    @Provide
    fun generatePropertyTester(
        @ForAll graph: TestGraph,
    ): Arbitrary<OneItemGraphPropertyTester> {
        val target = Arbitraries.of(graph.vertexSet())
        return target.map { OneItemGraphPropertyTester(it, graph) }
    }
}

class OneItemGraphPropertyTester(
    val targetNode: TestNode,
    originalGraph: TestGraph,
) : TestGraphPropertyTester(originalGraph) {
    override fun OptionRaise.testImpl(
        retainedCut: GraphCut<TestNode>,
        deletedCut: GraphCut<TestNode>,
    ) {
        ensure(targetNode in retainedCut)
    }
}
