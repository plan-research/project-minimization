package org.plan.research.minimization.plugin.psi.stub

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtProperty
import java.util.*

class KtPropertyStub(override val name: String?, val receiverTypeText: String?, val typeParams: String?) : KtStub() {
    override val descriptor: StubDescriptor
        get() = StubDescriptor.PROPERTY

    override fun equals(other: Any?): Boolean {
        if (other !is KtPropertyStub) {
            return false
        }
        return name == other.name && receiverTypeText == other.receiverTypeText && typeParams == other.typeParams
    }

    override fun hashCode(): Int = Objects.hash(descriptor, name, receiverTypeText, typeParams)

    override fun toString(): String =
        "KtPropertyStub(name=$name, receiverTypeText=$receiverTypeText, typeParams=$typeParams)"

    override fun getNext(element: PsiElement): PsiElement? = element
        .children
        .asSequence()
        .filterIsInstance<KtProperty>()
        .find { create(it) == this }

    companion object {
        fun create(element: KtProperty) = KtPropertyStub(
            element.name,
            element.receiverTypeReference?.getTypeText(),
            element.typeParameterList?.text ?: "",
        )
    }
}
