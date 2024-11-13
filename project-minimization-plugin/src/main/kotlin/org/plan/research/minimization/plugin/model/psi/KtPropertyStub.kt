package org.plan.research.minimization.plugin.model.psi

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty
import java.util.*

class KtPropertyStub(override val name: String?): KtStub() {
    companion object {
        fun create(element: KtProperty) = KtPropertyStub(element.name)
    }

    override val descriptor: StubDescriptor
        get() = StubDescriptor.Property

    override fun equals(other: Any?): Boolean {
        if (other !is KtPropertyStub) return false
        return name == other.name
    }

    override fun hashCode(): Int {
        return Objects.hash(descriptor, name)
    }

    override fun toString(): String {
        return "KtPropertyStub(name=$name)"
    }

    override fun getNext(element: PsiElement): PsiElement? = element
        .children
        .asSequence()
        .filterIsInstance<KtProperty>()
        .find { create(it) == this }
}