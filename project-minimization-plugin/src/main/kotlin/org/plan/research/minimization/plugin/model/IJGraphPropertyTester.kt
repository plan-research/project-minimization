package org.plan.research.minimization.plugin.model

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.PropertyTesterWithGraph
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.monad.SnapshotMonad

interface IJGraphPropertyTester<C : IJDDContext, T : DDItem> : PropertyTesterWithGraph<SnapshotMonad<C>, T>
