package org.plan.research.minimization.core.algorithm.dd.impl.graph.domain

import arrow.core.raise.OptionRaise
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.ForAll
import net.jqwik.api.Provide
import net.jqwik.api.domains.DomainContextBase
import net.jqwik.kotlin.api.ofSize
import org.plan.research.minimization.core.algorithm.dd.impl.graph.TestGraph
import org.plan.research.minimization.core.algorithm.dd.impl.graph.TestGraphPropertyTester
import org.plan.research.minimization.core.algorithm.dd.impl.graph.TestNode
import org.plan.research.minimization.core.model.GraphCut
import kotlin.math.min

class MultipleItemsPropertyTesterDomain : DomainContextBase() {
    @Provide
    fun generatePropertyTester(
        @ForAll graph: TestGraph,
    ): Arbitrary<MultipleItemsGraphPropertyTester> {
        val maxSize = min(graph.vertexSet().size, 10)
        val minSize = min(graph.vertexSet().size, 2)
        val target = Arbitraries.of(graph.vertexSet()).list()
            .uniqueElements()
            .ofSize(minSize..maxSize)
        return target.map { MultipleItemsGraphPropertyTester(it, graph) }
    }
}

class MultipleItemsGraphPropertyTester(
    val targetNodes: List<TestNode>,
    originalGraph: TestGraph,
) : TestGraphPropertyTester(originalGraph) {
    override fun OptionRaise.testImpl(
        retainedCut: GraphCut<TestNode>,
        deletedCut: GraphCut<TestNode>,
    ) {
        ensure(retainedCut.containsAll(targetNodes))
    }
}
