package org.plan.research.minimization.plugin.modification.psi.stub

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtParameterList

object KtParameterListStub : KtStub() {
    override val name = null
    override val descriptor = StubDescriptor.PARAMETER_LIST

    override fun getNext(element: PsiElement) = element.children.find { it is KtParameterList }
    override fun toString(): String = "KtParameterListStub"
    override fun equals(other: Any?) = other is KtParameterListStub

    override fun hashCode() = descriptor.hashCode()
}
