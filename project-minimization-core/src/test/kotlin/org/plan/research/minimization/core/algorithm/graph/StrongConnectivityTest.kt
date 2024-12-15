package org.plan.research.minimization.core.algorithm.graph

import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.domains.Domain
import org.plan.research.minimization.core.algorithm.graph.condensation.CondensedEdge
import org.plan.research.minimization.core.algorithm.graph.condensation.CondensedGraph
import org.plan.research.minimization.core.algorithm.graph.condensation.CondensedVertex
import org.plan.research.minimization.core.algorithm.graph.condensation.CondensedVertexSet
import org.plan.research.minimization.core.algorithm.graph.condensation.StrongConnectivityCondensation
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private typealias CondensedSet = CondensedVertexSet<TestNode, TestEdge, TestGraph>
private typealias CondensedV = CondensedVertex<TestNode>
private typealias CondensedE = CondensedEdge<TestNode, TestEdge>
private typealias CondensedG = CondensedGraph<TestNode, TestEdge, TestGraph>

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
    @Property(tries = 100)
    @Domain(TestGraphDomain::class)
    fun `test on random graph`(@ForAll graph: TestGraph) {
        doTest(graph)
    }

    private fun doTest(graph: TestGraph) {
        val condensation = StrongConnectivityCondensation.compressGraph(graph)
        condensation.componentsDoNotIntersect()
        condensation.allVerticesInComponents()
        condensation.isDag()
        condensation.components.forEach {
            assertTrue(
                message = "Component $it is not strongly connected"
            ) {
                condensation.isStronglyConnected(it)
            }
            condensation.isMaximumComponentSize(it)
        }
    }

    private fun CondensedSet.isStronglyConnected(component: CondensedV): Boolean {
        val vertices = component.underlyingVertexes.toSet()

        for (vertex in vertices) {
            val connectivityChecker = object : DepthFirstGraphWalkerVoid<TestNode, TestEdge, TestGraph, Int>() {
                private var visitedNumber = 0
                override fun onUnvisitedNode(graph: TestGraph, node: TestNode) {
                    if (node in vertices) visitedNumber++
                    super.onUnvisitedNode(graph, node)
                }

                override fun onComplete(graph: TestGraph) = visitedNumber
            }
            val visited = connectivityChecker.visitComponent(originalGraph, vertex)
            if (vertices.size != visited)
                return false
        }
        return true
    }

    private fun CondensedSet.componentsDoNotIntersect() {
        components.forEach { component ->
            components.forEach { otherComponent ->
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

    private fun CondensedSet.isMaximumComponentSize(component: CondensedV) {
        val possibleVertices = originalGraph.vertices.toSet() - component.underlyingVertexes.toSet()
        for (vertice in possibleVertices) {
            assertFalse(
                message = "Adding $vertice to $component left it strongly connected => That's not a maximum component"
            ) { this.isStronglyConnected(CondensedV(component.underlyingVertexes + vertice)) }
        }
    }
    private fun CondensedSet.allVerticesInComponents() {
        assertEquals(originalGraph.vertices.size, components.sumOf { it.underlyingVertexes.size }, "Not all vertices are in components")
    }
    private fun CondensedSet.isDag() {
        val graph = CondensedGraph.from(this)
        val cycleChecker = object : DepthFirstGraphWalkerVoid<CondensedV, CondensedE, CondensedG, Boolean>() {
            private var visitedColors = mutableMapOf<CondensedV, Int>()
            private var hasCycle = false
            override fun onUnvisitedNode(graph: CondensedG, node: CondensedV) {
                visitedColors[node] = 1
                super.onUnvisitedNode(graph, node)
                visitedColors[node] = 2
            }

            override fun onPassedEdge(graph: CondensedG, edge: CondensedE) {
                hasCycle = hasCycle || visitedColors[edge.to] == 1
            }

            override fun onComplete(graph: CondensedG): Boolean = hasCycle
        }
        assertFalse { cycleChecker.visitGraph(graph) }
    }
}