package org.plan.research.minimization.core.algorithm.dd.impl.graph

import kotlinx.coroutines.runBlocking
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.PropertyDefaults
import net.jqwik.api.ShrinkingMode
import net.jqwik.api.domains.Domain
import org.jgrapht.graph.AsSubgraph
import org.jgrapht.traverse.DepthFirstIterator
import org.junit.jupiter.api.Assertions.assertEquals
import org.plan.research.minimization.core.algorithm.dd.DDGraphAlgorithm
import org.plan.research.minimization.core.algorithm.dd.DDGraphAlgorithmResult
import org.plan.research.minimization.core.algorithm.dd.impl.graph.domain.*
import org.plan.research.minimization.core.model.EmptyMonad

@PropertyDefaults(tries = 500, shrinking = ShrinkingMode.BOUNDED)
abstract class DDGraphAlgorithmTestBase {
    @Property
    @Domain(TreeDomain::class)
    @Domain(OneItemPropertyTesterDomain::class)
    fun testSingleOnTree(@ForAll propertyTester: OneItemGraphPropertyTester) {
        val tree = propertyTester.originalGraph

        val (retainedCut, deletedCut) = runAlgorithm(tree, propertyTester)
        checkCuts(tree, retainedCut, deletedCut)

        val retainedGraph = AsSubgraph(tree, retainedCut)
        assert(propertyTester.targetNode in retainedCut)
        assert(retainedCut.all {
            retainedGraph.outDegreeOf(it) <= 1 && retainedGraph.inDegreeOf(it) <= 1
        })
        assert(retainedGraph.inDegreeOf(propertyTester.targetNode) == 0)
        val depthFirstIterator = DepthFirstIterator(retainedGraph, propertyTester.targetNode)
        var count = 0
        depthFirstIterator.forEach { _ -> count++ }
        assertEquals(retainedCut.size, count)
    }

    @Property
    @Domain(DAGDomain::class)
    @Domain(OneItemPropertyTesterDomain::class)
    fun testSingleOnDag(@ForAll propertyTester: OneItemGraphPropertyTester) {
        val graph = propertyTester.originalGraph

        val (retainedCut, deletedCut) = runAlgorithm(graph, propertyTester)
        checkCuts(graph, retainedCut, deletedCut)

        assert(propertyTester.targetNode in retainedCut)
    }

    @Property
    @Domain(TreeDomain::class)
    @Domain(MultipleItemsPropertyTesterDomain::class)
    fun testMultipleOnTree(@ForAll propertyTester: MultipleItemsGraphPropertyTester) {
        val tree = propertyTester.originalGraph

        val (retainedCut, deletedCut) = runAlgorithm(tree, propertyTester)
        checkCuts(tree, retainedCut, deletedCut)

        assert(retainedCut.containsAll(propertyTester.targetNodes))
    }

    @Property
    @Domain(DAGDomain::class)
    @Domain(MultipleItemsPropertyTesterDomain::class)
    fun testMultipleOnDag(@ForAll propertyTester: MultipleItemsGraphPropertyTester) {
        val graph = propertyTester.originalGraph

        val (retainedCut, deletedCut) = runAlgorithm(graph, propertyTester)
        checkCuts(graph, retainedCut, deletedCut)

        assert(retainedCut.containsAll(propertyTester.targetNodes))
    }

    private fun runAlgorithm(
        graph: TestGraph,
        propertyTester: TestGraphPropertyTester,
    ): DDGraphAlgorithmResult<TestNode> = runBlocking {
        EmptyMonad.run { getAlgorithm().minimize(graph, propertyTester) }
    }

    abstract fun getAlgorithm(): DDGraphAlgorithm
}
