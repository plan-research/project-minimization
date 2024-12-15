package org.plan.research.minimization.core.algorithm.graph

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Combinators
import net.jqwik.api.Provide
import net.jqwik.api.domains.DomainContextBase
import net.jqwik.kotlin.api.any

class TestGraphTreeDomain : DomainContextBase() {
    @Provide
    fun generateTree(): Arbitrary<TestGraph> {
        val treeSize = Int.any(1..500)
        val treeEdges = treeSize.flatMap { size ->
            val edgeList = List(size - 1) { to ->
                val range = 0..to
                Int.any(range).map { from -> TestEdge(TestNode("$from"), TestNode("${to + 1}")) } // from < to
            }
            Combinators.combine(edgeList).`as` { it }
        }
        return treeEdges.map { edges ->
            val nodes = edges.flatMap { listOf(it.from, it.to) }.distinct()
            TestGraph(nodes, edges)
        }
    }
}