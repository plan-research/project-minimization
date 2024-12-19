package org.plan.research.minimization.plugin.psi

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtVisitorVoid

class UsedPsiElementGetter(private val insideFunction: Boolean) : KtVisitorVoid() {
    private var collectedReferences: MutableList<PsiElement> = mutableListOf()
    val usedElements: List<PsiElement>
        get() = collectedReferences

    override fun visitClass(klass: KtClass) = Unit
    override fun visitObjectDeclaration(declaration: KtObjectDeclaration) = Unit
    override fun visitNamedFunction(function: KtNamedFunction) = Unit
    override fun visitProperty(property: KtProperty) = if (insideFunction) super.visitProperty(property) else Unit
    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)
    }

    override fun visitKtElement(element: KtElement) {
        super.visitKtElement(element)
        collectedReferences.addAll(
            KotlinElementLookup.lookupEverything(element)
        )
        element.acceptChildren(this)
    }
}