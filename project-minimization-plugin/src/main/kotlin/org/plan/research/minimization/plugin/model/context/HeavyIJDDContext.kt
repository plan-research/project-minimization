package org.plan.research.minimization.plugin.model.context

import org.plan.research.minimization.plugin.psi.KtSourceImportRefCounter

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.util.progress.SequentialProgressReporter

/**
 * This context represents a project as an opened IntelliJ IDEA project.
 */
class HeavyIJDDContext(
    val project: Project,
    originalProject: Project = project,
    progressReporter: SequentialProgressReporter? = null,
    importRefCounter: KtSourceImportRefCounter? = null,
) : IJDDContext(
    originalProject,
    progressReporter,
    importRefCounter,
) {
    override val projectDir: VirtualFile by lazy { project.guessProjectDir()!! }
    override val indexProject: Project = project

    fun asLightContext(): LightIJDDContext = LightIJDDContext(
        projectDir, indexProject = project,
        originalProject = originalProject,
        progressReporter, importRefCounter,
    )

    fun copy(
        project: Project,
        progressReporter: SequentialProgressReporter? = this.progressReporter,
        importRefCounter: KtSourceImportRefCounter? = this.importRefCounter,
    ): HeavyIJDDContext = HeavyIJDDContext(
        project,
        originalProject,
        progressReporter,
        importRefCounter,
    )

    override fun copy(
        progressReporter: SequentialProgressReporter?,
        importRefCounter: KtSourceImportRefCounter?,
    ): HeavyIJDDContext = copy(project, progressReporter, importRefCounter)

    override fun toString(): String = "HeavyIJDDContext(project=$projectDir)"
}
