package org.plan.research.minimization.core.model.graph

import org.plan.research.minimization.core.model.DDItem

interface DirectedGraph<V : DDItem, E : GraphEdge<V>> : Graph<V, E>
