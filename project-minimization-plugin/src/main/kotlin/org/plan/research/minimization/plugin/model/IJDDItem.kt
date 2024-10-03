package org.plan.research.minimization.plugin.model

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.plan.research.minimization.core.model.DDItem
import java.nio.file.Path
import kotlin.io.path.relativeTo

sealed interface IJDDItem : DDItem

data class ProjectFileDDItem(val localPath: Path) : IJDDItem {
    fun getVirtualFile(context: IJDDContext): VirtualFile? =
        context.projectDir.findFileByRelativePath(localPath.toString())

    companion object {
        fun create(context: IJDDContext, virtualFile: VirtualFile): ProjectFileDDItem =
            ProjectFileDDItem(virtualFile.toNioPath().relativeTo(context.projectDir.toNioPath()))
    }
}

data class PsiDDItem(val psi: PsiElement) : IJDDItem
