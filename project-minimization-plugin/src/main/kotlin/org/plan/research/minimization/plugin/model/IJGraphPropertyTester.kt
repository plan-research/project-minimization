package org.plan.research.minimization.plugin.model

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.GraphPropertyTester
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.monad.SnapshotMonad

interface IJGraphPropertyTester<C : IJDDContext, T : DDItem> : GraphPropertyTester<SnapshotMonad<C>, T>
