package org.plan.research.minimization.plugin.psi.stub

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtPrimaryConstructor

object KtPrimaryConstructorStub : KtStub() {
    override val name = null
    override val descriptor = StubDescriptor.PRIMARY_CONSTRUCTOR

    override fun getNext(element: PsiElement) = element.children.find { it is KtPrimaryConstructor }
    override fun toString(): String = "KtPrimaryConstructorStub"
    override fun equals(other: Any?) = other is KtPrimaryConstructorStub

    override fun hashCode() = KtBlockExpressionStub.descriptor.hashCode()
}
