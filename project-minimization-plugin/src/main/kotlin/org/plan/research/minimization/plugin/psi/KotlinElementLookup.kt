package org.plan.research.minimization.plugin.psi

import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.plan.research.minimization.plugin.psi.lookup.DefinitionAndCallDeclarationLookup
import org.plan.research.minimization.plugin.psi.lookup.ExpectDeclarationLookup
import org.plan.research.minimization.plugin.psi.lookup.TypeDeclarationLookup

object KotlinElementLookup {
    @RequiresReadLock
    fun lookupType(element: PsiElement): List<PsiElement> = TypeDeclarationLookup.getSymbolTypeDeclarations(element)

    @RequiresReadLock
    fun lookupDefinition(element: PsiElement): List<PsiElement> = DefinitionAndCallDeclarationLookup
        .getReferenceDeclaration(element)

    @RequiresReadLock
    fun lookupExpected(element: PsiElement) = ExpectDeclarationLookup.lookupExpect(element)

    @RequiresReadLock
    fun lookupEverything(element: PsiElement) =
        lookupDefinition(element) + listOfNotNull(lookupType(element)) + (lookupExpected(element) ?: emptyList())
}