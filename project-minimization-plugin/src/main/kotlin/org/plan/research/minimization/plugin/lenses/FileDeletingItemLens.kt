package org.plan.research.minimization.plugin.lenses

import org.plan.research.minimization.plugin.model.ProjectItemLens
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.context.IJDDContextMonad
import org.plan.research.minimization.plugin.model.item.ProjectFileDDItem

import com.intellij.openapi.application.writeAction

class FileDeletingItemLens : ProjectItemLens<IJDDContext, ProjectFileDDItem> {
    context(IJDDContextMonad<C>)
    override suspend fun <C : IJDDContext> focusOn(items: List<ProjectFileDDItem>) {
        val targetFiles = context
            .currentLevel
            ?.minus(items.toSet())
            ?.filterIsInstance<ProjectFileDDItem>()
            ?: return

        writeAction {
            targetFiles.forEach { item ->
                item.getVirtualFile(context)?.delete(this)
            }
        }
    }
}
