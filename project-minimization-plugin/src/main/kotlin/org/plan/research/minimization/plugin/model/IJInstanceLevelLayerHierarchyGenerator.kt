package org.plan.research.minimization.plugin.model

import org.plan.research.minimization.core.algorithm.graph.hierarchical.ReversedGraphLayerHierarchyProducer
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.core.model.graph.GraphWithAdjacencyList
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.monad.IJContextWithProgressMonad
import org.plan.research.minimization.plugin.model.monad.IJDDContextMonad

abstract class IJInstanceLevelLayerHierarchyGenerator<C : IJDDContext, V : DDItem, E : GraphEdge<V>, G : GraphWithAdjacencyList<V, E, G>>(
    propertyTester: IJGraphPropertyTester<C, V>,
) :
    ReversedGraphLayerHierarchyProducer<V, E, G, IJContextWithProgressMonad<C>, IJDDContextMonad<C>>(propertyTester)
