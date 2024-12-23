package org.plan.research.minimization.plugin.lenses

import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.ProjectFileDDItem
import org.plan.research.minimization.plugin.model.ProjectItemLens

import com.intellij.openapi.application.writeAction

class FileDeletingItemLens : ProjectItemLens<ProjectFileDDItem> {
    override suspend fun focusOn(items: List<ProjectFileDDItem>, currentContext: IJDDContext): IJDDContext {
        val targetFiles = currentContext
            .currentLevel
            ?.minus(items.toSet())
            ?.filterIsInstance<ProjectFileDDItem>()
            ?: return currentContext

        writeAction {
            targetFiles.forEach { item ->
                item.getVirtualFile(currentContext)?.delete(this)
            }
        }
        return currentContext
    }
}
