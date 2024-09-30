package org.plan.research.minimization.plugin.model.snapshot

import com.intellij.openapi.project.Project
import org.plan.research.minimization.plugin.model.dd.IJDDContext
import org.plan.research.minimization.plugin.model.dd.IJDDItem

/**
 * Functional interface that represents a function that shrinks a snapshot project with selected items
 *
 * @param T the type of items that the modifier operates on, which extends [IJDDItem].
 */
@FunctionalInterface
interface ProjectShrinkProducer<T: IJDDItem> {
    fun modifyWith(context: IJDDContext, items: List<T>): (suspend (Project) -> (Unit))?
}