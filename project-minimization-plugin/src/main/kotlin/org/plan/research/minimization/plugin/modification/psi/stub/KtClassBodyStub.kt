package org.plan.research.minimization.plugin.modification.psi.stub

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClassBody

object KtClassBodyStub : KtStub() {
    override val descriptor: StubDescriptor = StubDescriptor.CLASS_BODY
    override val name = null

    override fun getNext(element: PsiElement) = element.children.find { it is KtClassBody }
    override fun compareTo(other: KtStub) = 0
    override fun toString(): String = "KtClassBody"
    override fun equals(other: Any?) = other is KtClassBodyStub

    override fun hashCode() = descriptor.hashCode()
}
