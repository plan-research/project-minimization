package org.plan.research.minimization.core.algorithm.dd.impl.graph.domain

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Provide
import net.jqwik.api.domains.DomainContextBase
import net.jqwik.kotlin.api.any
import org.plan.research.minimization.core.algorithm.dd.impl.graph.TestGraph
import org.plan.research.minimization.core.algorithm.dd.impl.graph.TestNode

class DAGDomain : DomainContextBase() {
    @Provide
    fun generateDags(): Arbitrary<TestGraph> {
        val treeGenerator = TreeDomain().generateTree()
        return treeGenerator.flatMap { tree ->
            val treeSize = tree.vertexSet().size
            if (treeSize <= 2) {
                return@flatMap treeGenerator
            }
            val allEdges = (0 until (treeSize - 1)).mapNotNull { to ->
                if (tree.outgoingEdgesOf(TestNode(to)).size == (treeSize - to - 1)) return@mapNotNull null
                Int
                    .any(to + 1 until treeSize)
                    .map { from -> TestNode(from) to TestNode(to) }
                    .filter { !tree.containsEdge(it.first, it.second) }
            }

            val maximumNumberOfEdges = treeSize * (treeSize - 1) / 2
            val additionalEdges = Arbitraries.oneOf(allEdges).list().uniqueElements().ofMaxSize(maximumNumberOfEdges)
            additionalEdges.map { edges ->
                val newTree = tree.clone() as TestGraph
                edges.forEach { newTree.addEdge(it.first, it.second) }
                newTree
            }
        }
    }
}