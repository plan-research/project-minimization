package org.plan.research.minimization.core.algorithm.graph.hierarchical

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.Monad
import org.plan.research.minimization.core.model.graph.Graph
import org.plan.research.minimization.core.model.graph.GraphCut
import org.plan.research.minimization.core.model.graph.GraphEdge

interface LayerToCutTransformer<V, E, G, M> where V : DDItem, E : GraphEdge<V>, G : Graph<V, E, G>, M : Monad {
    context(M)
    suspend fun transform(layer: List<V>): GraphCut<V>
}
