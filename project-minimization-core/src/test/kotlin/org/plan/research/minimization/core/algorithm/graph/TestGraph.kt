package org.plan.research.minimization.core.algorithm.graph

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.getOrNone
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.core.model.graph.GraphWithAdjacencyList


data class TestNode(val id: String) : DDItem
data class TestEdge(override val from: TestNode, override val to: TestNode) : GraphEdge<TestNode>()

data class TestGraph(override val vertices: List<TestNode>, override val edges: List<TestEdge>) :
    GraphWithAdjacencyList<TestNode, TestEdge>() {
    private val inDegrees = edges.groupBy { it.to }.mapValues { it.value.size }
    override fun inDegreeOf(vertex: TestNode): Int = inDegrees[vertex] ?: 0

    override fun edgesFrom(vertex: TestNode): Option<List<TestEdge>> = adjacencyList.getOrNone(vertex)
}
