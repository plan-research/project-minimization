package org.plan.research.minimization.plugin.psi.lookup

import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.sourcePsi
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedDeclaration

object ExpectDeclarationLookup {
    @OptIn(KaExperimentalApi::class)
    @RequiresReadLock
    fun lookupExpect(element: PsiElement): List<PsiElement> {
        if (element !is KtNamedDeclaration) return emptyList()
        return analyze(element) {
            val symbol = element.symbol
            (symbol.getExpectsForActual()).mapNotNull { (it.sourcePsi() as? KtDeclaration)}
        }
    }
}