package org.plan.research.minimization.plugin.model.context

import org.plan.research.minimization.plugin.psi.KtSourceImportRefCounter

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.util.progress.SequentialProgressReporter

/**
 * This context represents a project as a usual project in the file system.
 */
class LightIJDDContext(
    override val projectDir: VirtualFile,
    override val indexProject: Project,
    originalProject: Project = indexProject,
    progressReporter: SequentialProgressReporter? = null,
    importRefCounter: KtSourceImportRefCounter? = null,
) : IJDDContext(
    originalProject,
    progressReporter,
    importRefCounter,
) {
    constructor(project: Project) : this(project.guessProjectDir()!!, project)

    fun copy(
        projectDir: VirtualFile,
        progressReporter: SequentialProgressReporter? = this.progressReporter,
        importRefCounter: KtSourceImportRefCounter? = this.importRefCounter,
    ): LightIJDDContext = LightIJDDContext(
        projectDir,
        indexProject,
        originalProject,
        progressReporter,
        importRefCounter,
    )

    override fun copy(
        progressReporter: SequentialProgressReporter?,
        importRefCounter: KtSourceImportRefCounter?,
    ): LightIJDDContext = copy(projectDir, progressReporter, importRefCounter)

    override fun toString(): String = "LightIJDDContext(project=$projectDir, indexProject=$indexProjectDir)"
}
