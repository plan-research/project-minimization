package org.plan.research.minimization.plugin.model.context

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile

@Suppress("KDOC_EXTRA_PROPERTY", "KDOC_NO_CLASS_BODY_PROPERTIES_IN_HEADER")
/**
 * Represents a context for the minimization process containing the current project and derived properties.
 *
 * @property originalProject The original project before any minimization stages.
 * @property indexProject The project that can be used for indexes or for progress reporting purposes
 * @constructor projectDir The directory of the current project to be minimized.
 */
sealed class IJDDContext(
    val originalProject: Project,
) {
    abstract val projectDir: VirtualFile
    abstract val indexProject: Project
    val indexProjectDir: VirtualFile by lazy { indexProject.guessProjectDir()!! }
}
