package org.plan.research.minimization.plugin.model.snapshot

import com.intellij.openapi.project.Project
import org.plan.research.minimization.plugin.model.dd.IJDDContext
import org.plan.research.minimization.plugin.model.dd.IJDDItem

@FunctionalInterface
interface ProjectModifier<T: IJDDItem> {
    fun modifyWith(context: IJDDContext, items: List<T>): (suspend (Project) -> (Unit))?
}