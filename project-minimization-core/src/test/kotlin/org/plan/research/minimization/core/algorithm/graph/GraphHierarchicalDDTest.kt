package org.plan.research.minimization.core.algorithm.graph

import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.raise.ensure
import kotlinx.coroutines.runBlocking
import net.jqwik.api.AfterFailureMode
import net.jqwik.api.Arbitrary
import net.jqwik.api.ForAll
import net.jqwik.api.GenerationMode
import net.jqwik.api.Property
import net.jqwik.api.Provide
import net.jqwik.api.ShrinkingMode
import net.jqwik.kotlin.api.anySubset
import org.junit.jupiter.api.Test
import org.plan.research.minimization.core.algorithm.dd.impl.ProbabilisticDD
import org.plan.research.minimization.core.algorithm.graph.condensation.CondensedGraph
import org.plan.research.minimization.core.algorithm.graph.condensation.CondensedVertex
import org.plan.research.minimization.core.algorithm.graph.domain.TestGraphDAGDomain
import org.plan.research.minimization.core.algorithm.graph.domain.TestGraphDomain
import org.plan.research.minimization.core.algorithm.graph.domain.TestGraphTreeDomain
import org.plan.research.minimization.core.algorithm.graph.hierarchical.DeletedDependenciesCollector
import org.plan.research.minimization.core.algorithm.graph.hierarchical.GraphHierarchicalDD
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.core.model.PropertyTesterError
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private typealias TransposedG = TransposedGraph<CondensedV, CondensedE, CondensedG>

open class GraphHierarchicalDDTest {
    @Provide
    private fun generateTree(): Arbitrary<Pair<TestGraph, Set<TestNode>>> =
        modifyArbitrary(TestGraphTreeDomain().generateTree())

    @Provide
    private fun generateDag(): Arbitrary<Pair<TestGraph, Set<TestNode>>> =
        modifyArbitrary(TestGraphDAGDomain().generateDags())

    @Provide
    private fun generateRandomGraph(): Arbitrary<Pair<TestGraph, Set<TestNode>>> =
        modifyArbitrary(TestGraphDomain().generateGraph())

    private fun modifyArbitrary(generator: Arbitrary<TestGraph>): Arbitrary<Pair<TestGraph, Set<TestNode>>> {
        return generator.flatMap { graph ->
            val nodes = graph.vertices
            if (nodes.isEmpty()) return@flatMap generator.map { it to emptySet() }
            nodes
                .anySubset()
                .ofMinSize(1)
                .uniqueElements()
                .map { graph to it }
        }
    }

    @Property(tries = 100, shrinking = ShrinkingMode.BOUNDED, generation = GenerationMode.AUTO)
    fun `test graph minimization for trees`(@ForAll("generateTree") minimizationPair: Pair<TestGraph, Set<TestNode>>) {
        doTest(minimizationPair.first, minimizationPair.second)
    }

    @Property(tries = 100, shrinking = ShrinkingMode.BOUNDED, generation = GenerationMode.AUTO)
    fun `test graph minimization for DAGs`(@ForAll("generateDag") minimizationPair: Pair<TestGraph, Set<TestNode>>) {
        doTest(minimizationPair.first, minimizationPair.second)
    }

    @Property(tries = 100, shrinking = ShrinkingMode.FULL, generation = GenerationMode.AUTO)
    fun `test graph minimization for random graphs`(@ForAll("generateRandomGraph") minimizationPair: Pair<TestGraph, Set<TestNode>>) {
        doTest(minimizationPair.first, minimizationPair.second)
    }

    private fun doTest(graph: TestGraph, selectedNodes: Set<TestNode>) = runBlocking {
        val algorithm = GraphHierarchicalDD<TestNode, TestEdge, TestGraph, TestContext>(ProbabilisticDD())
        val result = algorithm.minimize(TestContext(graph = graph), TestPropertyTester(selectedNodes.toSet()))
        val condensedGraph = requireNotNull(result.condensedGraph)

        condensedGraph.assertOnlyMeaningfulSourcesAreLeft(selectedNodes.toSet())
        condensedGraph.assertAllSelectedNodesAreSelected(selectedNodes.toSet())
    }

    private data class TestContext(
        override val graph: TestGraph? = null,
        override val condensedGraph: CondensedGraph<TestNode, TestEdge>? = null,
        override val currentLevel: List<DDItem>? = null
    ) : GraphContext<TestNode, TestEdge, TestGraph> {
        override fun copy(
            currentLevel: List<DDItem>?,
            condensedGraph: CondensedGraph<TestNode, TestEdge>?
        ) = copy(
            currentLevel = currentLevel,
            condensedGraph = condensedGraph,
            graph = this.graph
        )
    }

    private inner class TestPropertyTester(private val selectedNodes: Set<TestNode>) :
        PropertyTester<TestContext, CondensedVertex<TestNode>> {
        override suspend fun test(
            context: TestContext,
            items: List<CondensedVertex<TestNode>>
        ): PropertyTestResult<TestContext> = either {
            val graph = context.condensedGraph!!

            val items = items.toSet()
            val currentLevel = context.currentLevel
            val deletedItems = (currentLevel as List<CondensedV>).toSet() - items.toSet()
            val deletedDependenciesCollector = DeletedDependenciesCollector<TestNode, TestEdge>(deletedItems)
            val deletedWithDependencies = deletedDependenciesCollector.visitGraph(graph)

            val testGraph = graph.withoutNodes(deletedWithDependencies)
            graph.assertCut(deletedWithDependencies)
            ensure(
                testGraph
                    .vertices
                    .flatMap { it.underlyingVertexes }
                    .toSet()
                    .containsAll(selectedNodes)
            ) { PropertyTesterError.NoProperty }
            context.copy(condensedGraph = testGraph)
        }
    }

    private fun CondensedGraph<TestNode, TestEdge>.assertCut(selected: Set<CondensedVertex<TestNode>>) {
        val edges = vertices.flatMap { edgesFrom(it).getOrElse { emptyList() } }
        val nonCutEdges = edges.filter { it.to in selected && it.from !in selected }
        assertTrue(
            message = "The check of the graph with items $selected is not cut. The prohibited edges are $nonCutEdges"
        ) { nonCutEdges.isEmpty() }
    }

    private fun CondensedGraph<TestNode, TestEdge>.assertOnlyMeaningfulSourcesAreLeft(selected: Set<TestNode>) {
        sources.forEach { source ->
            assertTrue(
                message = "The source $source has no target nodes in it. So it should be discarded"
            ) { source.underlyingVertexes.any { it in selected } }
        }
    }

    private fun CondensedGraph<TestNode, TestEdge>.assertAllSelectedNodesAreSelected(selected: Set<TestNode>) {
        val flattened = vertices.flatMap { it.underlyingVertexes }.toSet()
        val intersection = flattened.intersect(selected)
        assertEquals(selected, intersection, "Not all selected nodes are selected")
    }
}