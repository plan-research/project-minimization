package org.plan.research.minimization.core.algorithm.graph.hierarchical

import org.plan.research.minimization.core.algorithm.graph.DepthFirstGraphWalkerVoid
import org.plan.research.minimization.core.algorithm.graph.condensation.CondensedEdge
import org.plan.research.minimization.core.algorithm.graph.condensation.CondensedGraph
import org.plan.research.minimization.core.algorithm.graph.condensation.CondensedVertex
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.GraphEdge

internal class DeletedDependenciesCollector<V : DDItem, E : GraphEdge<V>>(private val deletedElements: Set<CondensedVertex<V>>) :
    DepthFirstGraphWalkerVoid<
CondensedVertex<V>,
CondensedEdge<V, E>,
CondensedGraph<V, E>,
Set<CondensedVertex<V>>>() {
    private val collectedElements = mutableSetOf<CondensedVertex<V>>()
    override fun onUnvisitedNode(graph: CondensedGraph<V, E>, node: CondensedVertex<V>) {
        if (node in deletedElements) {
            collectedElements.add(node)
        } else {
            super.onUnvisitedNode(graph, node)
        }
    }

    override fun onPassedEdge(graph: CondensedGraph<V, E>, edge: CondensedEdge<V, E>) {
        if (edge.to in collectedElements) {
            collectedElements.add(edge.from)
        }
    }

    override fun onComplete(graph: CondensedGraph<V, E>) = collectedElements
}
