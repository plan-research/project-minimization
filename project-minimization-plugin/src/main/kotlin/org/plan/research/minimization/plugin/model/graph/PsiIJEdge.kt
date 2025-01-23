package org.plan.research.minimization.plugin.model.graph

import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.item.PsiStubDDItem
import org.plan.research.minimization.plugin.psi.PsiUtils

import arrow.core.raise.option
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.util.toPsiDirectory
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jgrapht.graph.DefaultEdge

sealed class PsiIJEdge : DefaultEdge() {
    abstract val from: PsiStubDDItem
    abstract val to: PsiStubDDItem

    class Overload(override val from: PsiStubDDItem, override val to: PsiStubDDItem) : PsiIJEdge()
    class PSITreeEdge(override val from: PsiStubDDItem, override val to: PsiStubDDItem) : PsiIJEdge()
    class UsageInPSIElement(override val from: PsiStubDDItem, override val to: PsiStubDDItem) : PsiIJEdge()
    class ObligatoryOverride(override val from: PsiStubDDItem, override val to: PsiStubDDItem) : PsiIJEdge()
    class FileTreeEdge(override val from: PsiStubDDItem, override val to: PsiStubDDItem) : PsiIJEdge() {
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
