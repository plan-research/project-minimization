package org.plan.research.minimization.plugin.psi.lookup

import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.psiSafe
import org.jetbrains.kotlin.analysis.api.symbols.sourcePsi
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtParameter

object ExpectDeclarationLookup {
    @OptIn(KaExperimentalApi::class)
    @RequiresReadLock
    fun lookupExpect(element: PsiElement): List<PsiElement> { // TODO: Direction?
        if (element !is KtNamedDeclaration) return emptyList()
        if (element is KtParameter) {
            val function = element.ownerFunction as? KtCallableDeclaration ?: return emptyList()
            val index = function.valueParameters.indexOf(element)
            return lookupExpect(function).mapNotNull { (it as? KtCallableDeclaration)?.valueParameters?.getOrNull(index) }
        }
        return analyze(element) {
            val symbol = element.symbol
            (symbol.getExpectsForActual()).mapNotNull { it.psiSafe<KtElement>()}
        }
    }
}