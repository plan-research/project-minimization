package org.plan.research.minimization.plugin.algorithm

import org.plan.research.minimization.plugin.compilation.exception.CompilationException
import org.plan.research.minimization.plugin.context.IJDDContext
import org.plan.research.minimization.plugin.modification.item.IJDDItem

typealias Listeners<T> = List<PropertyCheckingListener<T>>

interface PropertyCheckingListener<T : IJDDItem> {
    fun onEmptyLevel(context: IJDDContext) = Unit
    fun beforeFocus(context: IJDDContext, items: List<T>) = Unit
    fun onSuccessfulFocus(context: IJDDContext) = Unit
    fun onSuccessfulCompilation(context: IJDDContext) = Unit
    fun onFailedCompilation(context: IJDDContext, result: CompilationException) = Unit
    fun onComparedExceptions(
        context: IJDDContext,
        initialException: CompilationException,
        newException: CompilationException,
        result: Boolean,
    ) = Unit
}
