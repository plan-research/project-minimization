package org.plan.research.minimization.plugin.model

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import org.plan.research.minimization.core.model.DDContext

data class IJDDContext(
    val project: Project,
    val originalProject: Project = project,
    val currentLevel: List<ProjectFileDDItem>? = null,
) : DDContext {
    val projectDir: VirtualFile by lazy { project.guessProjectDir()!! }
}
