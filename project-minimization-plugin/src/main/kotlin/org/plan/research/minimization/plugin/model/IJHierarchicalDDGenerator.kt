package org.plan.research.minimization.plugin.model

import org.plan.research.minimization.core.algorithm.dd.hierarchical.ReversedHierarchicalDDGenerator
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.item.IJDDItem
import org.plan.research.minimization.plugin.model.monad.IJContextWithProgressMonad
import org.plan.research.minimization.plugin.model.monad.IJDDContextMonad

interface IJHierarchicalDDGenerator<C : IJDDContext, T : IJDDItem> :
    ReversedHierarchicalDDGenerator<IJContextWithProgressMonad<C>, IJDDContextMonad<C>, T>
