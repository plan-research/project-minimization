package org.plan.research.minimization.plugin.algorithm

import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDDGenerator
import org.plan.research.minimization.plugin.context.IJDDContext
import org.plan.research.minimization.plugin.context.snapshot.SnapshotMonad
import org.plan.research.minimization.plugin.modification.item.IJDDItem
import org.plan.research.minimization.plugin.util.SnapshotWithProgressMonad

interface IJHierarchicalDDGenerator<C : IJDDContext, T : IJDDItem> :
    HierarchicalDDGenerator<SnapshotWithProgressMonad<C>, SnapshotMonad<C>, T>
