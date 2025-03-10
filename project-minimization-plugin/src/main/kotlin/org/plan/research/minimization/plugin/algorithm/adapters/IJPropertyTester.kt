package org.plan.research.minimization.plugin.algorithm.adapters

import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.plugin.context.IJDDContext
import org.plan.research.minimization.plugin.context.snapshot.SnapshotMonad
import org.plan.research.minimization.plugin.modification.item.IJDDItem

interface IJPropertyTester<C : IJDDContext, T : IJDDItem> : PropertyTester<SnapshotMonad<C>, T>
