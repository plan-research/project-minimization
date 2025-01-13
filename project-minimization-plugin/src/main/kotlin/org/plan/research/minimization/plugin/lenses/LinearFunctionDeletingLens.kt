package org.plan.research.minimization.plugin.lenses

import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.PsiStubDDItem

class LinearFunctionDeletingLens : FunctionDeletingLens() {
    override fun currentLevel(context: IJDDContext): List<PsiStubDDItem> = context.currentLevel as List<PsiStubDDItem>
}
