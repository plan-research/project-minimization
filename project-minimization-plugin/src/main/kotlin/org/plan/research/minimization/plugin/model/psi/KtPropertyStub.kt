package org.plan.research.minimization.plugin.model.psi

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtProperty
import java.util.*

class KtPropertyStub(override val name: String?) : KtStub() {
    override val descriptor: StubDescriptor
        get() = StubDescriptor.PROPERTY

    override fun equals(other: Any?): Boolean {
        if (other !is KtPropertyStub) {
            return false
        }
        return name == other.name
    }

    override fun hashCode(): Int = Objects.hash(descriptor, name)

    override fun toString(): String = "KtPropertyStub(name=$name)"

    override fun getNext(element: PsiElement): PsiElement? = element
        .children
        .asSequence()
        .filterIsInstance<KtProperty>()
        .find { create(it) == this }
    companion object {
        fun create(element: KtProperty) = KtPropertyStub(element.name)
    }
}
