package org.plan.research.minimization.plugin.model.context

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.util.progress.SequentialProgressReporter
import org.plan.research.minimization.plugin.model.item.IJDDItem
import org.plan.research.minimization.plugin.psi.KtSourceImportRefCounter

/**
 * This context represents a project as a usual project in the file system.
 */
class LightIJDDContext(
    override val projectDir: VirtualFile,
    override val indexProject: Project,
    originalProject: Project = indexProject,
    currentLevel: List<IJDDItem>? = null,
    progressReporter: SequentialProgressReporter? = null,
    importRefCounter: KtSourceImportRefCounter? = null,
) : IJDDContext(
    originalProject,
    currentLevel,
    progressReporter,
    importRefCounter,
) {
    constructor(project: Project) : this(project.guessProjectDir()!!, project)

    fun copy(
        projectDir: VirtualFile,
        currentLevel: List<IJDDItem>? = this.currentLevel,
        progressReporter: SequentialProgressReporter? = this.progressReporter,
        importRefCounter: KtSourceImportRefCounter? = this.importRefCounter,
    ): LightIJDDContext = LightIJDDContext(
        projectDir,
        indexProject,
        originalProject,
        currentLevel,
        progressReporter,
        importRefCounter,
    )

    override fun copy(
        currentLevel: List<IJDDItem>?,
        progressReporter: SequentialProgressReporter?,
        importRefCounter: KtSourceImportRefCounter?,
    ): LightIJDDContext = copy(projectDir, currentLevel, progressReporter, importRefCounter)

    override fun toString(): String = "LightIJDDContext(project=$projectDir, indexProject=$indexProjectDir)"
}