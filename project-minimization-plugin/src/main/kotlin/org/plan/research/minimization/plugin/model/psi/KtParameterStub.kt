package org.plan.research.minimization.plugin.model.psi

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtParameter
import java.util.Objects

class KtParameterStub(
    override val name: String?,
    val typeId: String?
): KtStub() {
    companion object {
        fun create(element: KtParameter) = KtParameterStub(element.name, element.typeReference?.getTypeText())
    }

    override val descriptor: StubDescriptor
        get() = StubDescriptor.Parameter

    override fun equals(other: Any?): Boolean {
        if (other !is KtParameterStub) return false
        return name == other.name && typeId == other.typeId
    }

    override fun hashCode(): Int {
        return Objects.hash(descriptor, name, typeId)
    }

    override fun toString(): String {
        return "KtParameterStub(name=$name)"
    }

    override fun getNext(element: PsiElement): PsiElement? = element
        .children
        .asSequence()
        .filterIsInstance<KtParameter>()
        .find { create(it) == this }
}