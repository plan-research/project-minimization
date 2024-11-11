package org.plan.research.minimization.plugin.prototype.slicing

import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.backend.jvm.ir.psiElement
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.util.expectedDeclarationIfAny
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver
import org.plan.research.minimization.plugin.prototype.slicing.lookup.DefinitionAndCallDeclarationLookup
import org.plan.research.minimization.plugin.prototype.slicing.lookup.ExpectDeclarationLookup
import org.plan.research.minimization.plugin.prototype.slicing.lookup.TypeDeclarationLookup

object KtPsiLookupUtils {
    @RequiresReadLock
    fun lookupType(element: PsiElement): PsiElement? = TypeDeclarationLookup.getSymbolTypeDeclarations(element)

    @RequiresReadLock
    fun lookupDefinition(element: PsiElement): List<PsiElement> = DefinitionAndCallDeclarationLookup
        .getReferenceDeclaration(element)

    @RequiresReadLock
    fun lookupExpected(element: PsiElement) = ExpectDeclarationLookup.lookupExpect(element)

    @RequiresReadLock
    fun lookupEverything(element: PsiElement) =
        lookupDefinition(element) + listOfNotNull(lookupType(element)) + (lookupExpected(element) ?: emptyList())
}