package org.plan.research.minimization.plugin.model

import org.plan.research.minimization.core.algorithm.dd.hierarchical.ReversedHierarchicalDDGenerator
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.monad.IJDDContextMonad
import org.plan.research.minimization.plugin.model.item.IJDDItem
import org.plan.research.minimization.plugin.model.monad.WithProgressMonadT

interface IJHierarchicalDDGenerator<C : IJDDContext, T : IJDDItem> :
    ReversedHierarchicalDDGenerator<WithProgressMonadT<IJDDContextMonad<C>>, IJDDContextMonad<C>, T>
