package org.plan.research.minimization.plugin.model

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.util.progress.reportSequentialProgress
import org.plan.research.minimization.core.model.DDContextWithLevel
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.plugin.psi.KtSourceImportRefCounter
import org.plan.research.minimization.plugin.psi.graph.CondensedInstanceLevelGraph


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
    override val currentLevel: List<IJDDItem>?,
    val progressReporter: SequentialProgressReporter?,
    val importRefCounter: KtSourceImportRefCounter?,
    val graph: CondensedInstanceLevelGraph?, // FIXME: Add graph context
) : DDContextWithLevel<IJDDContext> {
    abstract val projectDir: VirtualFile
    abstract val indexProject: Project
    val indexProjectDir: VirtualFile by lazy { indexProject.guessProjectDir()!! }

    abstract fun copy(
        currentLevel: List<IJDDItem>? = this.currentLevel,
        progressReporter: SequentialProgressReporter? = this.progressReporter,
        importRefCounter: KtSourceImportRefCounter? = this.importRefCounter,
        graph: CondensedInstanceLevelGraph? = this.graph,
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
    graph: CondensedInstanceLevelGraph? = null,
) : IJDDContext(
    originalProject,
    currentLevel,
    progressReporter,
    importRefCounter,
    graph,
) {
    override val projectDir: VirtualFile by lazy { project.guessProjectDir()!! }
    override val indexProject: Project = project

    fun asLightContext(): LightIJDDContext = LightIJDDContext(
        projectDir, indexProject = project,
        originalProject = originalProject,
        currentLevel, progressReporter, importRefCounter, graph,
    )

    fun copy(
        project: Project,
        currentLevel: List<IJDDItem>? = this.currentLevel,
        progressReporter: SequentialProgressReporter? = this.progressReporter,
        importRefCounter: KtSourceImportRefCounter? = this.importRefCounter,
        graph: CondensedInstanceLevelGraph? = this.graph,
    ): HeavyIJDDContext = HeavyIJDDContext(
        project,
        originalProject,
        currentLevel,
        progressReporter,
        importRefCounter,
        graph,
    )

    override fun copy(
        currentLevel: List<IJDDItem>?,
        progressReporter: SequentialProgressReporter?,
        importRefCounter: KtSourceImportRefCounter?,
        graph: CondensedInstanceLevelGraph?,
    ): HeavyIJDDContext = copy(project, currentLevel, progressReporter, importRefCounter, graph)

    override fun toString(): String = "HeavyIJDDContext(project=$projectDir)"
    override fun withCurrentLevel(currentLevel: List<DDItem>): HeavyIJDDContext =
        copy(currentLevel = currentLevel as List<IJDDItem>?)
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
    graph: CondensedInstanceLevelGraph? = null,
) : IJDDContext(
    originalProject,
    currentLevel,
    progressReporter,
    importRefCounter,
    graph,
) {
    constructor(project: Project) : this(project.guessProjectDir()!!, project)

    fun copy(
        projectDir: VirtualFile,
        currentLevel: List<IJDDItem>? = this.currentLevel,
        progressReporter: SequentialProgressReporter? = this.progressReporter,
        importRefCounter: KtSourceImportRefCounter? = this.importRefCounter,
        graph: CondensedInstanceLevelGraph? = this.graph,
    ): LightIJDDContext = LightIJDDContext(
        projectDir,
        indexProject,
        originalProject,
        currentLevel,
        progressReporter,
        importRefCounter,
        graph,
    )

    override fun copy(
        currentLevel: List<IJDDItem>?,
        progressReporter: SequentialProgressReporter?,
        importRefCounter: KtSourceImportRefCounter?,
        graph: CondensedInstanceLevelGraph?,
    ): LightIJDDContext = copy(projectDir, currentLevel, progressReporter, importRefCounter, graph)

    override fun withCurrentLevel(currentLevel: List<DDItem>): LightIJDDContext =
         copy(currentLevel = currentLevel as List<IJDDItem>?)

    override fun toString(): String = "LightIJDDContext(project=$projectDir, indexProject=$indexProjectDir)"
}
