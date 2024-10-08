package org.plan.research.minimization.plugin.model

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import org.plan.research.minimization.core.model.DDContext
import org.plan.research.minimization.plugin.toVirtualFiles


/**
 * Represents a context for the minimization process containing the current project and derived properties.
 *
 * @property project The current project to be minimized.
 * @property originalProject The original project before any minimization stages, defaults to [project].
 * @property currentLevel An optional list of [ProjectFileDDItem] representing the current level of the minimizing project files.
 */
data class IJDDContext(
    val project: Project,
    val originalProject: Project = project,
    val currentLevel: List<ProjectFileDDItem>? = null,
) : DDContext {
    val projectDir: VirtualFile by lazy { project.guessProjectDir()!! }
    val currentLevelVirtualFiles: List<VirtualFile>? by lazy { currentLevel?.toVirtualFiles(this) }
}
