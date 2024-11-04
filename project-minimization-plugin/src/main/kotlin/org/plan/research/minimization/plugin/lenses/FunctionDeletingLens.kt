package org.plan.research.minimization.plugin.lenses

import org.plan.research.minimization.plugin.model.IJDDContext

import com.intellij.openapi.application.writeAction
import com.intellij.psi.PsiElement

class FunctionDeletingLens : BasePsiLens() {
    override suspend fun focusOnPsiElement(
        psiElement: PsiElement,
        context: IJDDContext,
    ) {
        writeAction { psiElement.delete() }
    }
}
