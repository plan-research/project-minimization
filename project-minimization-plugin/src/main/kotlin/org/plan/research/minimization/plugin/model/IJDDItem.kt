package org.plan.research.minimization.plugin.model

import com.intellij.openapi.vfs.VirtualFile
import org.plan.research.minimization.core.model.DDItem

sealed interface IJDDItem : DDItem {
    data class VirtualFileDDItem(val vfs: VirtualFile): IJDDItem
}
