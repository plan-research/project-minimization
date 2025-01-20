package org.plan.research.minimization.plugin.model

import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.item.IJDDItem
import org.plan.research.minimization.plugin.model.monad.IJDDContextMonad

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
