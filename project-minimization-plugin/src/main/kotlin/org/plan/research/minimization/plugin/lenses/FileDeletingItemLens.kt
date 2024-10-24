package org.plan.research.minimization.plugin.lenses

import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.IJDDItem
import org.plan.research.minimization.plugin.model.ProjectFileDDItem
import org.plan.research.minimization.plugin.model.ProjectItemLens

import com.intellij.openapi.application.writeAction
import mu.KotlinLogging

class FileDeletingItemLens : ProjectItemLens {
    private val logger = KotlinLogging.logger { }

    override suspend fun focusOn(items: List<IJDDItem>, currentContext: IJDDContext) {
        val targetFiles = currentContext
            .currentLevel
            ?.minus(items.toSet())
            ?.filterIsInstance<ProjectFileDDItem>()
            ?: return

        writeAction {
            targetFiles.forEach { item ->
                item.getVirtualFile(currentContext)?.let {
                    logger.debug { "Deleting ${item.localPath}, $it" }
                    it.delete(this)
                }
            }
        }
    }
}
