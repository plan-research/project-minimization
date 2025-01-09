package org.plan.research.minimization.plugin.model.context

import org.plan.research.minimization.core.model.DDContextMonad

class IJDDContextMonad<C : IJDDContext>(context : C) : DDContextMonad<C>(context) {
    fun createSubMonad(newContext: C): IJDDContextMonad<C> {
        return IJDDContextMonad(newContext)
    }
}
