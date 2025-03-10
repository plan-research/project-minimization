package org.plan.research.minimization.plugin.modification.lenses

import org.plan.research.minimization.plugin.context.IJDDContext
import org.plan.research.minimization.plugin.context.IJDDContextMonad
import org.plan.research.minimization.plugin.modification.item.IJDDItem

/**
 * Interface that specifies a way how the property tester focuses on selected files on that level
 */
interface ProjectItemLens<C : IJDDContext, I : IJDDItem> {
    /**
     * Focus on the [itemsToDelete].
     * The focusing process might require making changes to the context, so it returns a (modified) context
     *
     * @param itemsToDelete items to focus on
     */
    context(IJDDContextMonad<C>)
    suspend fun focusOn(itemsToDelete: List<I>)
}
