package org.plan.research.minimization.plugin.snapshot

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectRootManager
import org.plan.research.minimization.plugin.getAllNestedElements
import org.plan.research.minimization.plugin.getAllParents
import org.plan.research.minimization.plugin.model.dd.IJDDContext
import org.plan.research.minimization.plugin.model.dd.IJDDItem
import org.plan.research.minimization.plugin.model.snapshot.ProjectModifier
import kotlin.io.path.relativeTo

@Service(Service.Level.PROJECT)
class VirtualFileProjectModifier(private val project: Project) : ProjectModifier<IJDDItem.VirtualFileDDItem> {
    override fun modifyWith(
        context: IJDDContext,
        items: List<IJDDItem.VirtualFileDDItem>
    ): (suspend (Project) -> Unit)? {
        val originalProjectRoot = project.guessProjectDir() ?: return null

        val allItems = items.map { it.vfs }.getAllParents(originalProjectRoot) +
                items.flatMap { it.vfs.getAllNestedElements() }
        val relativeItemsPath = allItems.map { it.toNioPath().relativeTo(originalProjectRoot.toNioPath()) }.toSet()

        return { project ->
            writeAction {
                val index = ProjectRootManager.getInstance(project).fileIndex
                val projectRootPath = project.guessProjectDir()?.toNioPath()
                if (projectRootPath != null) {
                    index.iterateContent { file ->
                        val relativeFilePath = file.toNioPath().relativeTo(projectRootPath)
                        if (file.exists() && relativeFilePath !in relativeItemsPath) {
                            file.delete(this)
                        }
                        true
                    }
                }
            }
            true
        }
    }
}