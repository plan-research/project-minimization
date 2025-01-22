package org.plan.research.minimization.core.algorithm.dd.impl.graph

import org.jgrapht.graph.AbstractBaseGraph
import org.jgrapht.graph.DefaultEdge
import org.plan.research.minimization.core.model.*

typealias TestGraph = AbstractBaseGraph<TestNode, DefaultEdge>

data class TestNode(val id: Int) : DDItem
