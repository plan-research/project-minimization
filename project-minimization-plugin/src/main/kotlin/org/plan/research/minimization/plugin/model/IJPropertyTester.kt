package org.plan.research.minimization.plugin.model

import org.plan.research.minimization.core.model.ReversedPropertyTester
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.context.IJDDContextMonad
import org.plan.research.minimization.plugin.model.item.IJDDItem

interface IJPropertyTester<C : IJDDContext, T : IJDDItem> : ReversedPropertyTester<IJDDContextMonad<C>, T>
