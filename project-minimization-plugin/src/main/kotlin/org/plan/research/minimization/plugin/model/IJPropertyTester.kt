package org.plan.research.minimization.plugin.model

import org.plan.research.minimization.core.model.ReversedPropertyTester
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.item.IJDDItem
import org.plan.research.minimization.plugin.model.monad.IJDDContextMonad

interface IJPropertyTester<C : IJDDContext, T : IJDDItem> : ReversedPropertyTester<IJDDContextMonad<C>, T>
