package org.plan.research.minimization.core.algorithm.graph

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Provide
import net.jqwik.api.domains.DomainContextBase
import net.jqwik.kotlin.api.any

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
            val allEdges = (0 until (treeSize - 1)).map { from ->
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