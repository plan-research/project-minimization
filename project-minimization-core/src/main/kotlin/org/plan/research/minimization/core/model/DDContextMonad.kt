package org.plan.research.minimization.core.model

open class DDContextMonad<C : DDContext>(var context: C) {
    inline fun updateContext(update: (C) -> C) {
        context = update(context)
    }
}
