package org.plan.research.minimization.plugin.model

/**
 * Interface that specifies a way how the property tester focuses on selected files on that level
 */
interface ProjectItemLens {
    suspend fun focusOn(items: List<IJDDItem>, currentContext: IJDDContext)
}
