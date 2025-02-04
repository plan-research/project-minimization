package org.plan.research.minimization.plugin.modification.psi.stub

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtBlockExpression

object KtBlockExpressionStub : KtStub() {
    override val descriptor: StubDescriptor = StubDescriptor.BLOCK_EXPRESSION
    override val name = null

    override fun getNext(element: PsiElement) = element.children.find { it is KtBlockExpression }
    override fun compareTo(other: KtStub) = 0
    override fun toString(): String = "KtBlockExpressionStub"
    override fun equals(other: Any?) = other is KtBlockExpressionStub

    override fun hashCode() = descriptor.hashCode()
}
