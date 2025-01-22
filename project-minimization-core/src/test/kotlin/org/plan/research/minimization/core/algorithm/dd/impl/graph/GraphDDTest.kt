package org.plan.research.minimization.core.algorithm.dd.impl.graph

import kotlinx.coroutines.runBlocking
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.PropertyDefaults
import net.jqwik.api.ShrinkingMode
import net.jqwik.api.domains.Domain
import org.jgrapht.traverse.DepthFirstIterator
import org.junit.jupiter.api.Assertions.assertEquals
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HDDLevel
import org.plan.research.minimization.core.algorithm.dd.impl.DDMin
import org.plan.research.minimization.core.algorithm.dd.impl.graph.domain.DAGDomain
import org.plan.research.minimization.core.algorithm.dd.impl.graph.domain.OneItemPropertyTesterDomain
import org.plan.research.minimization.core.algorithm.dd.impl.graph.domain.OneItemGraphPropertyTester
import org.plan.research.minimization.core.algorithm.dd.impl.graph.domain.TreeDomain
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.EmptyMonad
import org.plan.research.minimization.core.model.Monad

class TestGraphLayerMonadT<M : Monad, T : DDItem>(monad: M) : GraphLayerMonadT<M, T>(monad) {
    override fun onNextLevel(level: HDDLevel<M, T>) {}
}

object TestGraphLayerMonadTProvider : GraphDD.GraphLayerMonadTProvider {
    context(M)
    override fun <M : Monad, T : DDItem> provide(): GraphLayerMonadT<M, T> =
        TestGraphLayerMonadT(this@M)
}

@PropertyDefaults(tries = 1000, shrinking = ShrinkingMode.BOUNDED)
class GraphDDTest {

    @Property
    @Domain(TreeDomain::class)
    @Domain(OneItemPropertyTesterDomain::class)
    fun testSingleOnTree(@ForAll propertyTester: OneItemGraphPropertyTester) {
        val tree = propertyTester.originalGraph

        val (retainedCut, deletedCut) = runAlgorithm(tree, propertyTester)

        assert(propertyTester.targetNode in retainedCut.vertexSet())
        assert(retainedCut.vertexSet().all {
            retainedCut.outDegreeOf(it) <= 1 && retainedCut.inDegreeOf(it) <= 1
        })
        assert(retainedCut.inDegreeOf(propertyTester.targetNode) == 0)
        val depthFirstIterator = DepthFirstIterator(retainedCut, propertyTester.targetNode)
        var count = 0
        depthFirstIterator.forEach { _ -> count++ }
        assertEquals(retainedCut.vertexSet().size, count)
    }

    @Property
    @Domain(DAGDomain::class)
    @Domain(OneItemPropertyTesterDomain::class)
    fun testSingleOnDag(@ForAll propertyTester: OneItemGraphPropertyTester) {
        val tree = propertyTester.originalGraph

        val (retainedCut, deletedCut) = runAlgorithm(tree, propertyTester)

        assert(propertyTester.targetNode in retainedCut.vertexSet())
    }

    private fun runAlgorithm(graph: TestGraph, propertyTester: TestGraphPropertyTester) = runBlocking {
        EmptyMonad.run { GraphDD(DDMin(), TestGraphLayerMonadTProvider).minimize(graph, propertyTester) }
    }.also {
        checkCuts(graph, it.retained, it.deleted)
    }
}