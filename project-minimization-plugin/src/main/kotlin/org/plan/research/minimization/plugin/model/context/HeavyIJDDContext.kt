package org.plan.research.minimization.plugin.model.context

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile

/**
 * This context represents a project as an opened IntelliJ IDEA project.
 */
abstract class HeavyIJDDContext<C : HeavyIJDDContext<C>>(
    val project: Project,
    originalProject: Project,
) : IJDDContext(originalProject) {
    override val projectDir: VirtualFile by lazy { project.guessProjectDir()!! }
    override val indexProject: Project = project

    abstract fun copy(project: Project): C

    override fun toString(): String = "HeavyIJDDContext(project=$projectDir)"
}
