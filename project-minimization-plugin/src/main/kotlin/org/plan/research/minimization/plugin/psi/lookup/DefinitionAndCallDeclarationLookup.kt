package org.plan.research.minimization.plugin.psi.lookup

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.KaCallableMemberCall
import org.jetbrains.kotlin.analysis.api.resolution.calls
import org.jetbrains.kotlin.analysis.api.resolution.singleCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.psi
import org.jetbrains.kotlin.analysis.api.symbols.sourcePsi
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression

object DefinitionAndCallDeclarationLookup {
    fun getReferenceDeclaration(element: PsiElement): List<PsiElement> = when (element) {
//        is KtCallExpression -> getCallInfo(element)
        is KtReferenceExpression -> getReference(element)
        else -> emptyList()
    }

    private fun getReference(ref: KtReferenceExpression): List<PsiElement> = analyze(ref) {
        ref
            .mainReference
            .resolveToSymbols()
            .map { it.psi<PsiElement>()}
            .filter { it.language == KotlinLanguage.INSTANCE }
    }

    private fun getCallInfo(callRef: KtCallExpression): List<PsiElement> = analyze(callRef) {
        val callInfo = callRef.resolveToCall()
        callInfo
            ?.calls
            ?.filterIsInstance<KaCallableMemberCall<*, *>>()
            ?.mapNotNull { it.symbol.psi() }
            ?: emptyList()
    }
}