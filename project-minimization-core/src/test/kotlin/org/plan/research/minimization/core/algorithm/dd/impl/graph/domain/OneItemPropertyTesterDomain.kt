package org.plan.research.minimization.core.algorithm.dd.impl.graph.domain

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.ForAll
import net.jqwik.api.Provide
import net.jqwik.api.domains.DomainContextBase
import org.plan.research.minimization.core.algorithm.dd.impl.graph.TestGraph

class OneItemPropertyTesterDomain : DomainContextBase() {
    @Provide
    fun generatePropertyTester(
        @ForAll graph: TestGraph,
    ): Arbitrary<SingleGraphPropertyTester> {
        val target = Arbitraries.of(graph.vertexSet())
        return target.map { SingleGraphPropertyTester(it, graph) }
    }
}