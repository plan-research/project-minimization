package org.plan.research.minimization.plugin.model

import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDDGenerator
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.item.IJDDItem
import org.plan.research.minimization.plugin.model.monad.SnapshotMonad
import org.plan.research.minimization.plugin.model.monad.SnapshotWithProgressMonad

interface IJHierarchicalDDGenerator<C : IJDDContext, T : IJDDItem> :
    HierarchicalDDGenerator<SnapshotWithProgressMonad<C>, SnapshotMonad<C>, T>
