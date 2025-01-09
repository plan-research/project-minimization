package org.plan.research.minimization.plugin.lenses

import com.intellij.psi.PsiElement
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.item.PsiChildrenIndexDDItem
import org.plan.research.minimization.plugin.model.item.index.IntChildrenIndex
import org.plan.research.minimization.plugin.psi.PsiBodyReplacer

/**
 * A lens that focuses on functions within a project.
 * It ensures that relevant function elements
 * are marked, processed, and reset appropriately within the given context.
 */
class FunctionModificationLens : BasePsiLens<IJDDContext, PsiChildrenIndexDDItem, IntChildrenIndex>() {
    override fun focusOnPsiElement(item: PsiChildrenIndexDDItem, psiElement: PsiElement, context: IJDDContext) =
        PsiBodyReplacer(context).transform(item, psiElement)
}
