package org.plan.research.minimization.core.algorithm.dd.impl.graph

import org.jgrapht.Graph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.traverse.DepthFirstIterator
import org.junit.jupiter.api.Assertions.assertEquals
import org.plan.research.minimization.core.model.GraphCut

fun checkCuts(
    graph: Graph<TestNode, DefaultEdge>,
    retainedCut: GraphCut<TestNode, DefaultEdge>,
    deletedCut: GraphCut<TestNode, DefaultEdge>,
) {
    assertEquals(graph.vertexSet(), retainedCut.vertexSet() + deletedCut.vertexSet())
    assert(
        retainedCut.vertexSet()
            .intersect(deletedCut.vertexSet())
            .isEmpty()
    )

    val retained = retainedCut.vertexSet()
    val iterator = DepthFirstIterator(graph, retained)
    iterator.forEach {
        assert(it in retained)
    }
}
