package org.plan.research.minimization.plugin.model.context

import org.plan.research.minimization.plugin.psi.KtSourceImportRefCounter

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile

/**
 * This context represents a project as an opened IntelliJ IDEA project.
 */
class HeavyIJDDContext(
    val project: Project,
    originalProject: Project = project,
    importRefCounter: KtSourceImportRefCounter? = null,
) : IJDDContext(
    originalProject,
    importRefCounter,
) {
    override val projectDir: VirtualFile by lazy { project.guessProjectDir()!! }
    override val indexProject: Project = project

    fun asLightContext(): LightIJDDContext = LightIJDDContext(
        projectDir, indexProject = project,
        originalProject = originalProject,
        importRefCounter,
    )

    fun copy(
        project: Project,
        importRefCounter: KtSourceImportRefCounter? = this.importRefCounter,
    ): HeavyIJDDContext = HeavyIJDDContext(
        project,
        originalProject,
        importRefCounter,
    )

    override fun copy(
        importRefCounter: KtSourceImportRefCounter?,
    ): HeavyIJDDContext = copy(project, importRefCounter)

    override fun toString(): String = "HeavyIJDDContext(project=$projectDir)"
}
