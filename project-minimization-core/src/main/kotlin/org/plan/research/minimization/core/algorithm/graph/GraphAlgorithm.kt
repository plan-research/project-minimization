package org.plan.research.minimization.core.algorithm.graph

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.core.model.graph.GraphWithAdjacencyList

interface GraphAlgorithm<V : DDItem, E : GraphEdge<V>, G : GraphWithAdjacencyList<V, E>, C : GraphContext<V, E, G>> {
    suspend fun minimize(
        context: C,
        propertyTester: PropertyTester<C, V>,
    ): C
}
