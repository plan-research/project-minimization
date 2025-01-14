package org.plan.research.minimization.plugin.model

import org.plan.research.minimization.core.algorithm.dd.hierarchical.ReversedHierarchicalDDGenerator
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.context.IJDDContextMonad
import org.plan.research.minimization.plugin.model.item.IJDDItem

interface IJHierarchicalDDGenerator<C : IJDDContext, T : IJDDItem> :
    ReversedHierarchicalDDGenerator<IJDDContextMonad<C>, T>
