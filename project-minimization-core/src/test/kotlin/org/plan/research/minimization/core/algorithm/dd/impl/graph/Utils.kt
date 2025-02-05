package org.plan.research.minimization.core.algorithm.dd.impl.graph

import org.jgrapht.Graph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.traverse.DepthFirstIterator
import org.junit.jupiter.api.Assertions.assertEquals
import org.plan.research.minimization.core.model.GraphCut

fun checkCuts(
    graph: Graph<TestNode, DefaultEdge>,
    retainedCut: GraphCut<TestNode>,
    deletedCut: GraphCut<TestNode>,
) {
    assertEquals(graph.vertexSet(), retainedCut + deletedCut)
    assert(
        retainedCut
            .intersect(deletedCut)
            .isEmpty()
    )

    val iterator = DepthFirstIterator(graph, retainedCut)
    iterator.forEach {
        assert(it in retainedCut)
    }
}
