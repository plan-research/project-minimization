package org.plan.research.minimization.plugin.model

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.plan.research.minimization.core.model.DDItem

sealed interface IJDDItem : DDItem

data class VirtualFileDDItem(val vfs: VirtualFile) : IJDDItem
data class PsiDDItem(val psi: PsiElement) : IJDDItem
