package org.plan.research.minimization.plugin.algorithm

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.GraphPropertyTester
import org.plan.research.minimization.plugin.context.IJDDContext
import org.plan.research.minimization.plugin.context.snapshot.SnapshotMonad

interface IJGraphPropertyTester<C : IJDDContext, T : DDItem> : GraphPropertyTester<SnapshotMonad<C>, T>
