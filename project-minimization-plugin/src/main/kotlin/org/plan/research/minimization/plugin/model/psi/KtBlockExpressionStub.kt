package org.plan.research.minimization.plugin.model.psi

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtBlockExpression

object KtBlockExpressionStub : KtStub() {
    override val descriptor: StubDescriptor = StubDescriptor.BlockExpression
    override val name: String? = null

    override fun getNext(element: PsiElement): PsiElement? = element.children.find { it is KtBlockExpression }
    override fun compareTo(other: KtStub) = 0
    override fun toString(): String = "KtClassBody"
    override fun equals(other: Any?): Boolean {
        return other is KtBlockExpressionStub
    }

    override fun hashCode() = descriptor.hashCode()
}