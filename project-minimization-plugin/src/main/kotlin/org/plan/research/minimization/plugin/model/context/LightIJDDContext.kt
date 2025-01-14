package org.plan.research.minimization.plugin.model.context

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * This context represents a project as a usual project in the file system.
 */
abstract class LightIJDDContext<C : LightIJDDContext<C>>(
    override val projectDir: VirtualFile,
    override val indexProject: Project,
    originalProject: Project,
) : IJDDContext(originalProject) {

    abstract fun copy(projectDir: VirtualFile): C

    override fun toString(): String = "LightIJDDContext(project=$projectDir, indexProject=$indexProjectDir)"
}
