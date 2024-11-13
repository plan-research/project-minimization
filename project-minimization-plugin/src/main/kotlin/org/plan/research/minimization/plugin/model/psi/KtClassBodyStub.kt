package org.plan.research.minimization.plugin.model.psi

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClassBody

object KtClassBodyStub : KtStub() {
    override val descriptor: StubDescriptor = StubDescriptor.ClassBody
    override val name: String? = null

    override fun getNext(element: PsiElement): PsiElement? = element.children.find { it is KtClassBody }
    override fun compareTo(other: KtStub) = 0
    override fun toString(): String = "KtClassBody"
    override fun equals(other: Any?): Boolean {
        return other is KtClassBodyStub
    }

    override fun hashCode() = descriptor.hashCode()
}