package org.plan.research.minimization.plugin.model

/**
 * Interface that specifies a way how the property tester focuses on selected files on that level
 */
interface ProjectItemLens<I: IJDDItem> {
    /**
     * Focus on the [items] within [currentContext].
     * The focusing process might require making changes to the context, so it returns a (modified) context
     *
     * @param items items to focus on
     * @param currentContext within the context of focusing
     * @return a modified copy of the [currentContext] that represents a focused state
     */
    suspend fun focusOn(items: List<I>, currentContext: IJDDContext): IJDDContext
}
