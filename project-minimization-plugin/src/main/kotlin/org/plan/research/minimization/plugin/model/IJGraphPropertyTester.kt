package org.plan.research.minimization.plugin.model

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.ReversedPropertyTesterWithGraph
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.monad.IJDDContextMonad

interface IJGraphPropertyTester<C : IJDDContext, T : DDItem> : ReversedPropertyTesterWithGraph<IJDDContextMonad<C>, T>
