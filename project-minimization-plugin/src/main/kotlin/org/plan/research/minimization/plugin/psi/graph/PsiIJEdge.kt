package org.plan.research.minimization.plugin.psi.graph

import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.item.PsiStubDDItem
import org.plan.research.minimization.plugin.psi.PsiUtils

import arrow.core.raise.ensureNotNull
import arrow.core.raise.option
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.util.toPsiDirectory
import org.jetbrains.kotlin.idea.core.util.toPsiFile

sealed class PsiIJEdge : GraphEdge<PsiStubDDItem>() {
    data class Overload(override val from: PsiStubDDItem, override val to: PsiStubDDItem) : PsiIJEdge()
    data class PSITreeEdge(override val from: PsiStubDDItem, override val to: PsiStubDDItem) : PsiIJEdge()
    data class UsageInPSIElement(override val from: PsiStubDDItem, override val to: PsiStubDDItem) : PsiIJEdge()
    data class ObligatoryOverride(override val from: PsiStubDDItem, override val to: PsiStubDDItem) : PsiIJEdge()
    data class FileTreeEdge(override val from: PsiStubDDItem, override val to: PsiStubDDItem) : PsiIJEdge() {
        companion object {
            fun create(context: IJDDContext, from: VirtualFile, to: VirtualFile) = option {
                FileTreeEdge(
                    PsiUtils.buildDeletablePsiItem(context, ensureNotNull(from.findFileOrDirectory(context))).bind(),
                    PsiUtils.buildDeletablePsiItem(context, ensureNotNull(to.findFileOrDirectory(context))).bind(),
                )
            }
            private fun VirtualFile.findFileOrDirectory(context: IJDDContext) =
                toPsiFile(context.indexProject) ?: toPsiDirectory(context.indexProject)
        }
    }
}
