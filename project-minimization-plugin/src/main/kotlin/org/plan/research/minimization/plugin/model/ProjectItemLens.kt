package org.plan.research.minimization.plugin.model

import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.context.IJDDContextMonad
import org.plan.research.minimization.plugin.model.item.IJDDItem

/**
 * Interface that specifies a way how the property tester focuses on selected files on that level
 */
interface ProjectItemLens<in BC : IJDDContext, I : IJDDItem> {
    /**
     * Focus on the [items].
     * The focusing process might require making changes to the context, so it returns a (modified) context
     *
     * @param items items to focus on
     */
    context(IJDDContextMonad<C>)
    suspend fun <C : BC> focusOn(items: List<I>)
}
