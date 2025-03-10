package org.plan.research.minimization.plugin.modification.lenses

import org.plan.research.minimization.plugin.context.IJDDContext
import org.plan.research.minimization.plugin.modification.item.PsiChildrenIndexDDItem
import org.plan.research.minimization.plugin.modification.item.index.IntChildrenIndex
import org.plan.research.minimization.plugin.modification.psi.PsiBodyReplacer

import com.intellij.psi.PsiElement

/**
 * A lens that focuses on functions within a project.
 * It ensures that relevant function elements
 * are marked, processed, and reset appropriately within the given context.
 */
class FunctionModificationLens<C : IJDDContext> : BasePsiLens<C, PsiChildrenIndexDDItem, IntChildrenIndex>() {
    override fun focusOnPsiElement(item: PsiChildrenIndexDDItem, psiElement: PsiElement, context: C) =
        PsiBodyReplacer(context).transform(item, psiElement)

    override suspend fun focusOnFilesAndDirectories(
        itemsToDelete: List<PsiChildrenIndexDDItem>,
        context: C,
    ) = Unit
}
