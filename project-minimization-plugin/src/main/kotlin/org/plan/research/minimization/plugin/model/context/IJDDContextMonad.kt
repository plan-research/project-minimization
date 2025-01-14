package org.plan.research.minimization.plugin.model.context

import org.plan.research.minimization.core.model.Monad

class IJDDContextMonad<C : IJDDContext>(var context: C) : Monad {
    fun createSubMonad(newContext: C): IJDDContextMonad<C> = IJDDContextMonad(newContext)

    inline fun updateContext(block: (C) -> C) {
        context = block(context)
    }
}
