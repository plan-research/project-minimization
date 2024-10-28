package org.plan.research.minimization.plugin.model

import org.plan.research.minimization.core.model.DDItem

import com.intellij.openapi.vfs.VirtualFile

import java.nio.file.Path

import kotlin.io.path.relativeTo

sealed interface IJDDItem : DDItem

/**
 * Represents a project file item in the minimization process.
 *
 * @property localPath The local path of the file within the project directory.
 * @constructor Creates an instance of [ProjectFileDDItem] with the specified local path.
 */
data class ProjectFileDDItem(val localPath: Path) : IJDDItem {
    fun getVirtualFile(context: IJDDContext): VirtualFile? =
        context.projectDir.findFileByRelativePath(localPath.toString())

    companion object {
        fun create(context: IJDDContext, virtualFile: VirtualFile): ProjectFileDDItem =
            ProjectFileDDItem(virtualFile.toNioPath().relativeTo(context.projectDir.toNioPath()))

        fun createFromIndexProject(context: IJDDContext, virtualFile: VirtualFile): ProjectFileDDItem =
            ProjectFileDDItem(virtualFile.toNioPath().relativeTo(context.indexProjectDir.toNioPath()))
    }
}
