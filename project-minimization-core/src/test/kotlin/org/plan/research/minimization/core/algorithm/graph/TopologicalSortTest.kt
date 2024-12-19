package org.plan.research.minimization.core.algorithm.graph

import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.PropertyDefaults
import net.jqwik.api.ShrinkingMode
import net.jqwik.api.domains.Domain
import org.plan.research.minimization.core.algorithm.graph.condensation.TopologicalSort
import org.plan.research.minimization.core.algorithm.graph.domain.TestGraphDAGDomain
import org.plan.research.minimization.core.algorithm.graph.domain.TestGraphTreeDomain
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@PropertyDefaults(tries = 1000, shrinking = ShrinkingMode.BOUNDED)
class TopologicalSortTest {
    private fun TestGraph.isTopologicalSort(list: List<TestNode>): Boolean {
        val indexes = list.mapIndexed { index, node -> node to index }.toMap()
        for (edge in edges) {
            val indexFrom = indexes[edge.from]!!
            val indexTo = indexes[edge.to]!!
            if (indexFrom > indexTo) {
                return false
            }
        }
        return true
    }

    @Property
    @Domain(TestGraphTreeDomain::class)
    fun `test topological sort on trees`(@ForAll tree: TestGraph) {
        val topologicallySorter = TopologicalSort<TestNode, TestEdge, _>().visitGraph(tree)
        assertEquals(tree.vertices.size, topologicallySorter.size)
        assertTrue { tree.isTopologicalSort(topologicallySorter) }
    }
    @Property
    @Domain(TestGraphDAGDomain::class)
    fun `test topological sort on DAGs`(@ForAll dag: TestGraph) {
        val topologicallySorter = TopologicalSort<TestNode, TestEdge, _>().visitGraph(dag)
        assertEquals(dag.vertices.size, topologicallySorter.size)
        assertTrue { dag.isTopologicalSort(topologicallySorter) }
    }

}