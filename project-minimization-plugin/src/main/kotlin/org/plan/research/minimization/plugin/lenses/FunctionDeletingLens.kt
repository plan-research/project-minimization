package org.plan.research.minimization.plugin.lenses

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtFile
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.PsiStubDDItem
import org.plan.research.minimization.plugin.model.psi.KtStub

class FunctionDeletingLens : BasePsiLens<PsiStubDDItem, KtStub>() {
    override fun focusOnPsiElement(
        psiElement: PsiElement,
        context: IJDDContext,
    ) = psiElement.delete()

    override fun getWriteCommandActionName(
        psiFile: KtFile,
        context: IJDDContext,
    ): String = "Deleting PSI elements from ${psiFile.name}"
}
