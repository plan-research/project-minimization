package org.plan.research.minimization.core.algorithm.graph.hierarchical

import org.plan.research.minimization.core.model.DDContextWithLevel
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.Graph
import org.plan.research.minimization.core.model.graph.GraphCut
import org.plan.research.minimization.core.model.graph.GraphEdge

interface LayerToCutTransformer<V, E, G, C> where V : DDItem, E : GraphEdge<V>, G : Graph<V, E, G>, C : DDContextWithLevel<C> {
    fun transform(layer: List<V>, context: C): GraphCut<V>
}
