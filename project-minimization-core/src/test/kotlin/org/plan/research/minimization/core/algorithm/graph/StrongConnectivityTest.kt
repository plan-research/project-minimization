package org.plan.research.minimization.core.algorithm.graph

import kotlinx.coroutines.runBlocking
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.domains.Domain
import org.plan.research.minimization.core.algorithm.graph.condensation.CondensedEdge
import org.plan.research.minimization.core.algorithm.graph.condensation.CondensedGraph
import org.plan.research.minimization.core.algorithm.graph.condensation.CondensedVertex
import org.plan.research.minimization.core.algorithm.graph.condensation.StrongConnectivityCondensation
import org.plan.research.minimization.core.algorithm.graph.domain.TestGraphDAGDomain
import org.plan.research.minimization.core.algorithm.graph.domain.TestGraphDomain
import org.plan.research.minimization.core.algorithm.graph.domain.TestGraphTreeDomain
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

typealias CondensedV = CondensedVertex<TestNode>
typealias CondensedE = CondensedEdge<TestNode, TestEdge>
typealias CondensedG = CondensedGraph<TestNode, TestEdge>

class StrongConnectivityTest {

    @Property(tries = 100)
    @Domain(TestGraphTreeDomain::class)
    fun `test tree condensation`(@ForAll tree: TestGraph) {
        doTest(tree)
    }

    @Property(tries = 100)
    @Domain(TestGraphDAGDomain::class)
    fun `test DAG condensation`(@ForAll dag: TestGraph) {
        doTest(dag)
    }
    @Property(tries = 50)
    @Domain(TestGraphDomain::class)
    fun `test on random graph`(@ForAll graph: TestGraph) {
        doTest(graph)
    }

    private fun doTest(graph: TestGraph) = runBlocking {
        val condensation = StrongConnectivityCondensation.compressGraph(graph)
        condensation.componentsDoNotIntersect()
        condensation.allVerticesInComponents(graph)
        condensation.isDag()
        condensation.vertices.forEach {
            assertTrue(
                message = "Component $it is not strongly connected"
            ) {
                condensation.isStronglyConnected(graph, it)
            }
            condensation.isMaximumComponentSize(graph, it)
        }
    }

    private suspend fun CondensedG.isStronglyConnected(originalGraph: TestGraph, component: CondensedV): Boolean {
        val vertices = component.underlyingVertexes.toSet()

        for (vertex in vertices) {
            val connectivityChecker = object : DepthFirstGraphWalkerVoid<TestNode, TestEdge, TestGraph, Int>() {
                private var visitedNumber = 0
                override suspend fun onUnvisitedNode(graph: TestGraph, node: TestNode) {
                    if (node in vertices) visitedNumber++
                    super.onUnvisitedNode(graph, node)
                }

                override suspend fun onComplete(graph: TestGraph) = visitedNumber
            }
            val visited = connectivityChecker.visitComponent(originalGraph, vertex)
            if (vertices.size != visited)
                return false
        }
        return true
    }

    private fun CondensedG.componentsDoNotIntersect() {
        vertices.forEach { component ->
            vertices.forEach { otherComponent ->
                if (component !== otherComponent) {
                    assertFalse("components $component and $otherComponent intersect") {
                        component
                            .underlyingVertexes
                            .toSet()
                            .intersect(otherComponent.underlyingVertexes.toSet())
                            .isNotEmpty()
                    }
                }
            }
        }
    }

    private suspend fun CondensedG.isMaximumComponentSize(originalGraph: TestGraph, component: CondensedV) {
        val possibleVertices = originalGraph.vertices.toSet() - component.underlyingVertexes.toSet()
        for (vertice in possibleVertices) {
            assertFalse(
                message = "Adding $vertice to $component left it strongly connected => That's not a maximum component"
            ) { this.isStronglyConnected(originalGraph, CondensedV(component.underlyingVertexes + vertice)) }
        }
    }
    private fun CondensedG.allVerticesInComponents(originalGraph: TestGraph) {
        assertEquals(originalGraph.vertices.size, vertices.sumOf { it.underlyingVertexes.size }, "Not all vertices are in components")
    }
    private suspend fun CondensedG.isDag() {
        val cycleChecker = object : DepthFirstGraphWalkerVoid<CondensedV, CondensedE, CondensedG, Boolean>() {
            private var visitedColors = mutableMapOf<CondensedV, Int>()
            private var hasCycle = false
            override suspend fun onUnvisitedNode(graph: CondensedG, node: CondensedV) {
                visitedColors[node] = 1
                super.onUnvisitedNode(graph, node)
                visitedColors[node] = 2
            }

            override suspend fun onPassedEdge(graph: CondensedG, edge: CondensedE) {
                hasCycle = hasCycle || visitedColors[edge.to] == 1
            }

            override suspend fun onComplete(graph: CondensedG): Boolean = hasCycle
        }
        assertFalse { cycleChecker.visitGraph(this) }
    }
}