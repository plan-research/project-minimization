package org.plan.research.minimization.plugin.modification.psi

import org.plan.research.minimization.plugin.modification.psi.lookup.AbstractOverriddenLookup
import org.plan.research.minimization.plugin.modification.psi.lookup.DefinitionAndCallDeclarationLookup
import org.plan.research.minimization.plugin.modification.psi.lookup.ExpectDeclarationLookup
import org.plan.research.minimization.plugin.modification.psi.lookup.TypeDeclarationLookup

import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtSecondaryConstructor

object KotlinElementLookup {
    @RequiresReadLock
    fun lookupType(element: PsiElement): List<PsiElement> = TypeDeclarationLookup
        .getSymbolTypeDeclarations(element)
        .map { it.propagateToConstructor() }

    @RequiresReadLock
    fun lookupDefinition(element: PsiElement): List<PsiElement> = DefinitionAndCallDeclarationLookup
        .getReferenceDeclaration(element)
        .map { it.propagateToConstructor() }

    @RequiresReadLock
    fun lookupExpected(element: PsiElement) = ExpectDeclarationLookup
        .lookupExpect(element)
        .map { it.propagateToConstructor() }

    @RequiresReadLock
    fun lookupObligatoryOverrides(element: PsiElement) =
        AbstractOverriddenLookup.run {
            lookupDirectlyOverridden(element) + lookupFunInterfaces(element)
        }.map { it.propagateToConstructor() }

    /**
     * Looks up and combines definitions, type declarations, and expected declarations
     * related to the given PSI element and propagates constructor to the corresponding classes, if applicable.
     *
     * @param element The PSI element for which related declarations are to be looked up.
     * @return A list of PSI elements that includes the relevant definitions, type declarations,
     *         and expected declarations.
     */
    @RequiresReadLock
    fun lookupEverything(element: PsiElement): List<PsiElement> =
        lookupDefinition(element) + lookupType(element) + lookupExpected(element)

    private fun PsiElement.propagateToConstructor() = when (this) {
        is KtPrimaryConstructor -> parent  // KtPrimaryConstructor -> KtClass
        is KtSecondaryConstructor -> parent.parent  // KtSecondaryConstructor -> KtClassBody -> KtClass
        else -> this

    }
}
