package org.plan.research.minimization.plugin.model

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.util.progress.reportSequentialProgress
import org.plan.research.minimization.core.model.DDContext

/**
 * Represents a context for the minimization process containing the current project and derived properties.
 *
 * @property projectDir The directory of the current project to be minimized.
 * @property originalProject The original project before any minimization stages.
 * @property currentLevel An optional list of [ProjectFileDDItem] representing the current level of the minimizing project files.
 * @property progressReporter An optional progress reporter for the minimization process
 */
data class IJDDContext(
    val projectDir: VirtualFile,
    val originalProject: Project,
    val currentLevel: List<ProjectFileDDItem>? = null,
    val progressReporter: SequentialProgressReporter? = null,
) : DDContext {

    val originalProjectDir: VirtualFile = originalProject.guessProjectDir()!!

    suspend fun withProgress(action: suspend (IJDDContext) -> IJDDContext): IJDDContext =
        reportSequentialProgress { reporter ->
            val context = action(copy(progressReporter = reporter))
            context.copy(progressReporter = null)
        }
}
