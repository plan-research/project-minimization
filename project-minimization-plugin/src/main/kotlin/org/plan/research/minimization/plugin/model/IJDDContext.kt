package org.plan.research.minimization.plugin.model

import org.plan.research.minimization.core.model.DDContext

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.util.progress.reportSequentialProgress

@Suppress("KDOC_EXTRA_PROPERTY", "KDOC_NO_CLASS_BODY_PROPERTIES_IN_HEADER")
/**
 * Represents a context for the minimization process containing the current project and derived properties.
 *
 * @property originalProject The original project before any minimization stages.
 * @property currentLevel An optional list of [ProjectFileDDItem] representing the current level of the minimizing project files.
 * @property progressReporter An optional progress reporter for the minimization process
 * @property indexProject The project that can be used for indexes or for progress reporting purposes
 * @constructor projectDir The directory of the current project to be minimized.
 */
sealed class IJDDContext(
    val originalProject: Project,
    val currentLevel: List<ProjectFileDDItem>?,
    val progressReporter: SequentialProgressReporter?,
) : DDContext {
    abstract val projectDir: VirtualFile
    abstract val indexProject: Project
    val indexProjectDir: VirtualFile by lazy { indexProject.guessProjectDir()!! }

    abstract fun copy(
        currentLevel: List<ProjectFileDDItem>? = this.currentLevel,
        progressReporter: SequentialProgressReporter? = this.progressReporter,
    ): IJDDContext

    suspend fun withProgress(action: suspend (IJDDContext) -> IJDDContext): IJDDContext =
        reportSequentialProgress { reporter ->
            val context = action(copy(progressReporter = reporter))
            context.copy(progressReporter = null)
        }
}

/**
 * This context represents a project as an opened IntelliJ IDEA project.
 */
class HeavyIJDDContext(
    val project: Project,
    originalProject: Project,
    currentLevel: List<ProjectFileDDItem>? = null,
    progressReporter: SequentialProgressReporter? = null,
) : IJDDContext(originalProject, currentLevel, progressReporter) {
    override val projectDir: VirtualFile by lazy { project.guessProjectDir()!! }
    override val indexProject: Project = project

    fun copy(
        project: Project,
        currentLevel: List<ProjectFileDDItem>? = this.currentLevel,
        progressReporter: SequentialProgressReporter? = this.progressReporter,
    ): HeavyIJDDContext = HeavyIJDDContext(
        project,
        originalProject,
        currentLevel,
        progressReporter,
    )

    override fun copy(
        currentLevel: List<ProjectFileDDItem>?,
        progressReporter: SequentialProgressReporter?,
    ): HeavyIJDDContext = copy(project, currentLevel, progressReporter)
}

/**
 * This context represents a project as a usual project in the file system.
 */
class LightIJDDContext(
    override val projectDir: VirtualFile,
    originalProject: Project,
    currentLevel: List<ProjectFileDDItem>? = null,
    progressReporter: SequentialProgressReporter? = null,
) : IJDDContext(originalProject, currentLevel, progressReporter) {
    override val indexProject: Project = originalProject

    fun copy(
        projectDir: VirtualFile,
        currentLevel: List<ProjectFileDDItem>? = this.currentLevel,
        progressReporter: SequentialProgressReporter? = this.progressReporter,
    ): LightIJDDContext = LightIJDDContext(
        projectDir,
        originalProject,
        currentLevel,
        progressReporter,
    )

    override fun copy(
        currentLevel: List<ProjectFileDDItem>?,
        progressReporter: SequentialProgressReporter?,
    ): IJDDContext = copy(projectDir, currentLevel, progressReporter)
}
