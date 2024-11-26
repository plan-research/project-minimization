package org.plan.research.minimization.plugin.psi.stub

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtParameter
import java.util.Objects

class KtParameterStub(
    override val name: String?,
    val typeId: String?,
) : KtStub() {
    override val descriptor: StubDescriptor
        get() = StubDescriptor.PARAMETER

    override fun equals(other: Any?): Boolean {
        if (other !is KtParameterStub) {
            return false
        }
        return name == other.name && typeId == other.typeId
    }

    override fun hashCode(): Int = Objects.hash(descriptor, name, typeId)

    override fun toString(): String = "KtParameterStub(name=$name)"

    override fun getNext(element: PsiElement): PsiElement? = element
        .children
        .asSequence()
        .filterIsInstance<KtParameter>()
        .find { create(it) == this }
    companion object {
        fun create(element: KtParameter) = KtParameterStub(element.name, element.typeReference?.getTypeText())
    }
}
