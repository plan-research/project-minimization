package org.plan.research.minimization.plugin.model.psi

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClass
import java.util.Objects

data class KtClassStub(val classId: ClassId?) : KtStub() {
    override val name: String = classId?.relativeClassName?.asString() ?: "<null>"
    override val descriptor: StubDescriptor
        get() = StubDescriptor.Class

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
        fun create(clazz: KtClass): KtClassStub =
            KtClassStub(
                clazz.getClassId(),
            )
    }
}
