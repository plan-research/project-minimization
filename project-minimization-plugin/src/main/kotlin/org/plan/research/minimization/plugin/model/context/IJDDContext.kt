package org.plan.research.minimization.plugin.model.context

import org.plan.research.minimization.plugin.model.item.IJDDItem
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
 * @property currentLevel An optional list representing the current level of the minimizing project files.
 * @property progressReporter An optional progress reporter for the minimization process
 * @property indexProject The project that can be used for indexes or for progress reporting purposes
 * @constructor projectDir The directory of the current project to be minimized.
 */
sealed class IJDDContext(
    val originalProject: Project,
    val currentLevel: List<IJDDItem>?,
    val progressReporter: SequentialProgressReporter?,
    val importRefCounter: KtSourceImportRefCounter?,
) {
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
