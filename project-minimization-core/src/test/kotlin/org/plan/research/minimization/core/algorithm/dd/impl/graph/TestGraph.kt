package org.plan.research.minimization.core.algorithm.dd.impl.graph

import org.jgrapht.graph.AbstractBaseGraph
import org.jgrapht.graph.DefaultEdge
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.EmptyMonad
import org.plan.research.minimization.core.model.GraphPropertyTester

typealias TestGraph = AbstractBaseGraph<TestNode, DefaultEdge>
typealias TestGraphPropertyTester = GraphPropertyTester<EmptyMonad, TestNode, DefaultEdge>

data class TestNode(val id: Int) : DDItem
