package org.plan.research.minimization.plugin.lenses

import org.plan.research.minimization.plugin.model.context.WithCallTraceParameterCacheContext
import org.plan.research.minimization.plugin.model.context.WithImportRefCounterContext
import org.plan.research.minimization.plugin.model.item.PsiStubChildrenCompositionItem
import org.plan.research.minimization.plugin.model.item.index.InstructionLookupIndex

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.base.psi.deleteSingle
import org.jetbrains.kotlin.idea.refactoring.deleteSeparatingComma

class CallTraceDeletionLens<C> :
    AbstractImportRefLens<C, PsiStubChildrenCompositionItem, InstructionLookupIndex>() where C : WithImportRefCounterContext<C>, C : WithCallTraceParameterCacheContext<C> {
    override fun focusOnPsiElement(
        item: PsiStubChildrenCompositionItem,
        psiElement: PsiElement,
        context: C,
    ) {
        deleteSeparatingComma(psiElement)
        psiElement.deleteSingle()
    }

    override suspend fun focusOnFilesAndDirectories(
        itemsToDelete: List<PsiStubChildrenCompositionItem>,
        context: C,
    ) {
        if (itemsToDelete.isEmpty()) {
            return
        }
        throw UnsupportedOperationException("This lens should be not called on their own but only as part of FunctionDeletionLens")
    }
}
