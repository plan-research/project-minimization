package org.plan.research.minimization.plugin.model.monad

import org.plan.research.minimization.core.model.Monad
import org.plan.research.minimization.plugin.model.context.IJDDContext

class IJDDContextMonad<C : IJDDContext>(var context: C) : Monad {
    inline fun updateContext(block: (C) -> C) {
        context = block(context)
    }
}
