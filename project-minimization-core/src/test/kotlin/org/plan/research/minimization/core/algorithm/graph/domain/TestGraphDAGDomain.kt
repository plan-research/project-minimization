package org.plan.research.minimization.core.algorithm.graph.domain

import arrow.core.getOrElse
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Provide
import net.jqwik.api.domains.DomainContextBase
import net.jqwik.kotlin.api.any
import org.plan.research.minimization.core.algorithm.graph.TestEdge
import org.plan.research.minimization.core.algorithm.graph.TestGraph
import org.plan.research.minimization.core.algorithm.graph.TestNode
import kotlin.collections.plus

class TestGraphDAGDomain : DomainContextBase() {
    @Provide
    fun generateDags(): Arbitrary<TestGraph> {
        val treeGenerator = TestGraphTreeDomain().generateTree()
        return treeGenerator.flatMap { tree ->
            val treeSize = tree.vertices.size
            if (treeSize <= 2) {
                return@flatMap treeGenerator
            }
            val edges = tree.edges.toSet()
            val allEdges = (0 until (treeSize - 1)).mapNotNull { from ->
                if (tree.edgesFrom(tree.vertices[from]).getOrElse {emptyList()}.size == (treeSize - from - 1)) return@mapNotNull null
                Int
                    .any(from + 1 until treeSize)
                    .map { to -> TestEdge(TestNode("$from"), TestNode("$to")) }
                    .filter { it !in edges }
            }

            val maximumNumberOfEdges = treeSize * (treeSize - 1) / 2
            val additionalEdges = Arbitraries.oneOf(allEdges).list().uniqueElements().ofMaxSize(maximumNumberOfEdges)
            additionalEdges.map { TestGraph(tree.vertices, tree.edges + it) }
        }
    }
}