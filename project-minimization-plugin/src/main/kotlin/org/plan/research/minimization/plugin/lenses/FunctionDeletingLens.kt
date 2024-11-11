package org.plan.research.minimization.plugin.lenses

import org.plan.research.minimization.plugin.model.IJDDContext

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtFile

class FunctionDeletingLens : BasePsiLens() {
    override fun focusOnPsiElement(
        psiElement: PsiElement,
        context: IJDDContext,
    ) = psiElement.delete()

    override fun getWriteCommandActionName(
        psiFile: KtFile,
        context: IJDDContext,
    ): String = "Deleting PSI elements from ${psiFile.name}"
}
