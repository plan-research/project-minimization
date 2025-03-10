package org.plan.research.minimization.plugin.context

import org.plan.research.minimization.core.model.Monad

class IJDDContextMonad<C : IJDDContext>(var context: C) : Monad {
    inline fun updateContext(block: (C) -> C) {
        context = block(context)
    }
}
