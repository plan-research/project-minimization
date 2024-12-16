package org.plan.research.minimization.core.algorithm.graph.hierarchical

import org.plan.research.minimization.core.algorithm.graph.GraphContext
import org.plan.research.minimization.core.algorithm.graph.condensation.CondensedVertex
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.core.model.graph.GraphWithAdjacencyList

internal typealias GraphPropertyTester<C, V> = PropertyTester<C, CondensedVertex<V>>

private typealias GraphPropertyTestResults<V, E, G, C> = PropertyTestResult<GraphHierarchicalDDContext<V, E, G, C>>
internal class GraphHierarchyItemPropertyTesterBridge<V, E, G, C>(
    private val propertyTester: GraphPropertyTester<C, V>,
) : PropertyTester<GraphHierarchicalDDContext<V, E, G, C>, CondensedVertex<V>>
where V : DDItem,
E : GraphEdge<V>,
G : GraphWithAdjacencyList<V, E>,
C : GraphContext<V, E, G> {
    override suspend fun test(
        context: GraphHierarchicalDDContext<V, E, G, C>,
        items: List<CondensedVertex<V>>,
    ): GraphPropertyTestResults<V, E, G, C> =
        propertyTester
            .test(
                context = context.backingContext,
                items = items,
            )
            .map { context.copy(backingContext = it) }
}
