package org.plan.research.minimization.plugin.model

import org.plan.research.minimization.core.model.DDContext
import org.plan.research.minimization.plugin.psi.KtSourceImportRefCounter

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
    val currentLevel: List<IJDDItem>?,
    val progressReporter: SequentialProgressReporter?,
    val importRefCounter: KtSourceImportRefCounter?,
) : DDContext {
    abstract val projectDir: VirtualFile
    abstract val indexProject: Project
    val indexProjectDir: VirtualFile by lazy { indexProject.guessProjectDir()!! }

    abstract fun copy(
        currentLevel: List<IJDDItem>? = this.currentLevel,
        progressReporter: SequentialProgressReporter? = this.progressReporter,
        importRefCounter: KtSourceImportRefCounter? = this.importRefCounter,
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
    originalProject: Project = project,
    currentLevel: List<IJDDItem>? = null,
    progressReporter: SequentialProgressReporter? = null,
    importRefCounter: KtSourceImportRefCounter? = null,
) : IJDDContext(
    originalProject,
    currentLevel,
    progressReporter,
    importRefCounter,
) {
    override val projectDir: VirtualFile by lazy { project.guessProjectDir()!! }
    override val indexProject: Project = project

    fun asLightContext(): LightIJDDContext = LightIJDDContext(
        projectDir, indexProject = project,
        originalProject = originalProject,
        currentLevel, progressReporter,
    )

    fun copy(
        project: Project,
        currentLevel: List<IJDDItem>? = this.currentLevel,
        progressReporter: SequentialProgressReporter? = this.progressReporter,
        importRefCounter: KtSourceImportRefCounter? = this.importRefCounter,
    ): HeavyIJDDContext = HeavyIJDDContext(
        project,
        originalProject,
        currentLevel,
        progressReporter,
        importRefCounter,
    )

    override fun copy(
        currentLevel: List<IJDDItem>?,
        progressReporter: SequentialProgressReporter?,
        importRefCounter: KtSourceImportRefCounter?,
    ): HeavyIJDDContext = copy(project, currentLevel, progressReporter, importRefCounter)

    override fun toString(): String = "HeavyIJDDContext(project=$projectDir)"
}

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
