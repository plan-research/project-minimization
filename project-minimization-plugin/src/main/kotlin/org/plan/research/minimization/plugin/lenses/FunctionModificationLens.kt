package org.plan.research.minimization.plugin.lenses

import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.IntChildrenIndex
import org.plan.research.minimization.plugin.model.PsiChildrenIndexDDItem
import org.plan.research.minimization.plugin.psi.PsiBodyReplacer

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtFile

/**
 * A lens that focuses on functions within a project.
 * It ensures that relevant function elements
 * are marked, processed, and reset appropriately within the given context.
 */
class FunctionModificationLens : BasePsiLens<PsiChildrenIndexDDItem, IntChildrenIndex>() {
    override fun focusOnPsiElement(item: PsiChildrenIndexDDItem, psiElement: PsiElement, currentContext: IJDDContext) =
        PsiBodyReplacer(currentContext).transform(item, psiElement)

    override fun getWriteCommandActionName(
        psiFile: KtFile,
        context: IJDDContext,
    ): String = "Replacing bodies of PSI elements from ${psiFile.name}"
}
