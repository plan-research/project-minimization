package org.plan.research.minimization.core.algorithm.dd.impl.graph.domain

import net.jqwik.api.Arbitrary
import net.jqwik.api.Combinators
import net.jqwik.api.Provide
import net.jqwik.api.domains.DomainContextBase
import net.jqwik.kotlin.api.any
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.SimpleDirectedGraph
import org.jgrapht.util.SupplierUtil
import org.plan.research.minimization.core.algorithm.dd.impl.graph.TestGraph
import org.plan.research.minimization.core.algorithm.dd.impl.graph.TestNode

class TreeDomain : DomainContextBase() {
    @Provide
    fun generateTree(): Arbitrary<TestGraph> {
        val treeSize = Int.any(1..100)
        val treeEdges = treeSize.flatMap { size ->
            val edgeList = List(size - 1) { to ->
                val range = 0..to
                Int.any(range).map { from -> TestNode(to + 1) to TestNode(from) } // from < to
            }
            Combinators.combine(edgeList).`as` { it }
        }
        return treeEdges.map { edges ->
            val tree =
                SimpleDirectedGraph.createBuilder<TestNode, DefaultEdge>(SupplierUtil.createDefaultEdgeSupplier())

            (edges.flatMap { listOf(it.first, it.second) } + TestNode(0))
                .distinct()
                .forEach { tree.addVertex(it) }

            edges.forEach { tree.addEdge(it.first, it.second) }

            tree.build()
        }
    }
}
