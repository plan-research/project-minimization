package org.plan.research.minimization.plugin.modification.psi.stub

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClass
import java.util.Objects

data class KtClassStub(override val name: String) : KtStub() {
    override val descriptor: StubDescriptor
        get() = StubDescriptor.CLASS

    override fun equals(other: Any?): Boolean {
        if (other !is KtClassStub) {
            return false
        }
        return name == other.name
    }

    override fun hashCode(): Int = Objects.hash(descriptor, name)

    override fun toString(): String = "KtClassStub(name=$name)"

    override fun getNext(element: PsiElement): PsiElement? = element
        .children
        .asSequence()
        .filterIsInstance<KtClass>()
        .find { create(it) == this }

    companion object {
        fun create(clazz: KtClass): KtClassStub = KtClassStub(clazz.name ?: "<null>")
    }
}
