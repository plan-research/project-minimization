package org.plan.research.minimization.plugin.modification.psi

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtVisitorVoid

/**
 * A utility class for collecting used PSI elements within a Kotlin PSI structure.
 * This class
 * traverses a given Kotlin PSI hierarchy and gathers relevant PSI elements based on its configuration.
 *
 * @param insideFunction A boolean flag indicating whether the traversal should consider elements
 * from within the body of a function.
 * For example, if this is set to true, then the properties will be omitted,
 * since local variables and class properties are not distinguishable
 */
class UsedPsiElementGetter(private val insideFunction: Boolean) : KtVisitorVoid() {
    private var collectedReferences: MutableList<PsiElement> = mutableListOf()
    val usedElements: List<PsiElement>
        get() = collectedReferences

    override fun visitClass(klass: KtClass) = Unit
    override fun visitObjectDeclaration(declaration: KtObjectDeclaration) = Unit
    override fun visitNamedFunction(function: KtNamedFunction) = function.name?.let {
        Unit
    } ?: super.visitNamedFunction(function)

    override fun visitProperty(property: KtProperty) =
        if (insideFunction) super.visitProperty(property) else Unit

    override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor) = Unit

    override fun visitKtElement(element: KtElement) {
        super.visitKtElement(element)
        collectedReferences.addAll(
            KotlinElementLookup.lookupDefinition(element),
        )
        element.acceptChildren(this)
    }
}
