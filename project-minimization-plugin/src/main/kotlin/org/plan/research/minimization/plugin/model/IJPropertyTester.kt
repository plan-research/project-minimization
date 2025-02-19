package org.plan.research.minimization.plugin.model

import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.item.IJDDItem
import org.plan.research.minimization.plugin.model.monad.SnapshotMonad

interface IJPropertyTester<C : IJDDContext, T : IJDDItem> : PropertyTester<SnapshotMonad<C>, T>
