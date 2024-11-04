package org.plan.research.minimization.plugin.lenses

import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.services.MinimizationPsiManager

import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement

/**
 * A lens that focuses on functions within a project.
 * It ensures that relevant function elements
 * are marked, processed, and reset appropriately within the given context.
 */
class FunctionModificationLens : BasePsiLens() {
    override suspend fun focusOnPsiElement(psiElement: PsiElement, currentContext: IJDDContext) {
        val psiManager = currentContext.project.service<MinimizationPsiManager>()
        psiManager.replaceBody(psiElement)
    }
}
