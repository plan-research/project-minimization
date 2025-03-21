package org.plan.research.minimization.plugin.modification.psi.lookup

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.psiSafe
import org.jetbrains.kotlin.idea.codeinsight.utils.isFunInterface
import org.jetbrains.kotlin.idea.refactoring.isAbstract
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClass

/**
 * A utility object for analyzing and retrieving directly overridden declarations
 * within Kotlin PsiElements.
 *
 * That object for each class provides a list of methods and properties that have been directly overridden from the abstract version in some superclass.
 */
object AbstractOverriddenLookup {
    private val KtClass.obligatoryFunctionIfFunInterface
        get() = body?.functions?.take(1)
            .takeIf { isFunInterface() }
            .orEmpty()
    fun lookupDirectlyOverridden(element: PsiElement): List<PsiElement> = when (element) {
        is KtClass -> element
            .declarations
            .filterIsInstance<KtCallableDeclaration>()
            .mapNotNull { it.getIfObligatoryImplementation() }

        else -> emptyList<PsiElement>()
    }
    fun lookupFunInterfaces(element: PsiElement): List<PsiElement> = when (element) {
        is KtClass -> element.obligatoryFunctionIfFunInterface
        else -> emptyList()
    }

    private fun KtCallableDeclaration.getIfObligatoryImplementation(): KtCallableDeclaration? =
        analyze(this) {
            val overrides = (this@getIfObligatoryImplementation.symbol as? KaCallableSymbol)
                ?.allOverriddenSymbols
                ?.mapNotNull { it.psiSafe<KtCallableDeclaration>() }  // FIXME: No place for Java
                ?.toList()
                ?: return null
            this@getIfObligatoryImplementation
                .takeIf { overrides.all { it.isAbstract() } && overrides.isNotEmpty() }
        }
}
