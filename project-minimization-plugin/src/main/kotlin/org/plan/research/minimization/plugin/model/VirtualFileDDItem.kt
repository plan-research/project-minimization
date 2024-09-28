package org.plan.research.minimization.plugin.model

import com.intellij.openapi.vfs.VirtualFile
import org.plan.research.minimization.core.model.DDItem

// TODO: think about make it sealed and create some different implementations, e.g. PSI or VF
data class VirtualFileDDItem(val vfs: VirtualFile): DDItem