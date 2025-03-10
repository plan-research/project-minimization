package org.plan.research.minimization.plugin.modification.lenses

import org.plan.research.minimization.plugin.context.IJDDContext
import org.plan.research.minimization.plugin.context.IJDDContextMonad
import org.plan.research.minimization.plugin.modification.item.ProjectFileDDItem

import com.intellij.openapi.application.writeAction

class FileDeletingItemLens<C : IJDDContext> : ProjectItemLens<C, ProjectFileDDItem> {
    context(IJDDContextMonad<C>)
    override suspend fun focusOn(itemsToDelete: List<ProjectFileDDItem>) {
        writeAction {
            itemsToDelete.forEach { item ->
                item.getVirtualFile(context)?.delete(this)
            }
        }
    }
}
