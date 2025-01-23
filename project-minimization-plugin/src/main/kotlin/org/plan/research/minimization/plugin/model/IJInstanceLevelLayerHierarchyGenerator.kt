package org.plan.research.minimization.plugin.model

import org.plan.research.minimization.core.algorithm.graph.hierarchical.GraphLayerHierarchyProducer
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.core.model.graph.GraphWithAdjacencyList
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.monad.SnapshotMonad
import org.plan.research.minimization.plugin.model.monad.SnapshotWithProgressMonad

abstract class IJInstanceLevelLayerHierarchyGenerator<C : IJDDContext, V : DDItem, E : GraphEdge<V>, G : GraphWithAdjacencyList<V, E, G>>(
    propertyTester: IJGraphPropertyTester<C, V>,
) : GraphLayerHierarchyProducer<V, E, G, SnapshotWithProgressMonad<C>, SnapshotMonad<C>>(propertyTester)
