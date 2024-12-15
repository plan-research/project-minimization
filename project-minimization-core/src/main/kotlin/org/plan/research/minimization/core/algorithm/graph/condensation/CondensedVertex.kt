package org.plan.research.minimization.core.algorithm.graph.condensation

import org.plan.research.minimization.core.model.DDItem

data class CondensedVertex<V : DDItem>(val underlyingVertexes: List<V>) : DDItem
