package org.plan.research.minimization.plugin.model

import com.intellij.openapi.vfs.VirtualFile
import org.plan.research.minimization.core.model.DDItem

sealed interface IJDDItem : DDItem {
    fun getChildren(): List<IJDDItem>
    data class VirtualFileDDItem(val vfs: VirtualFile): IJDDItem {
        override fun getChildren(): List<VirtualFileDDItem> = vfs.children.map { VirtualFileDDItem(it) }
    }
}
