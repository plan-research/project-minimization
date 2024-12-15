package org.plan.research.minimization.core.algorithm.graph

import net.jqwik.api.Arbitrary
import net.jqwik.api.Provide
import net.jqwik.api.domains.DomainContextBase
import net.jqwik.kotlin.api.any

class TestGraphDomain : DomainContextBase() {
    @Provide
    fun generateGraph(): Arbitrary<TestGraph> {
        val graphSizeGenerator = Int.any(1..500)
        return graphSizeGenerator.flatMap { graphSize ->
            val nodes = List(graphSize) { TestNode("$it") }
            val edges = Int
                .any(0 until graphSize)
                .flatMap { from ->
                    Int
                        .any(0 until graphSize)
                        .map { to ->
                            TestEdge(nodes[from], nodes[to])
                        }
                }.list().uniqueElements()
            edges.map { TestGraph(nodes, it) }
        }
    }
}