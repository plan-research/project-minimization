package org.plan.research.minimization.core.algorithm.graph.hierarchical

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDD
import org.plan.research.minimization.core.algorithm.dd.withZeroTesting
import org.plan.research.minimization.core.algorithm.graph.GraphContext
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.core.model.graph.GraphWithAdjacencyList

class GraphHierarchicalDD<V, E, G, C>(val baseDDAlgorithm: DDAlgorithm)
where V : DDItem,
E : GraphEdge<V>,
G : GraphWithAdjacencyList<V, E>,
C : GraphContext<V, E, G> {
    suspend fun minimize(
        context: C,
        propertyTester: GraphPropertyTester<C, V>,
    ): C {
        val generator = GraphHierarchyProducer<V, E, G, C>(GraphHierarchyItemPropertyTesterBridge(propertyTester))
        val hierarchicalDD = HierarchicalDD(baseDDAlgorithm.withZeroTesting())
        val context = hierarchicalDD.minimize(GraphHierarchicalDDContext(context), generator)
        return context.backingContext
    }
}
