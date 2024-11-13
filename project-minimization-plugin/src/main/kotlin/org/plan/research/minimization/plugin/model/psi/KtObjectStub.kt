package org.plan.research.minimization.plugin.model.psi

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import java.util.*

class KtObjectStub(
    val classId: ClassId?,
) : KtStub() {
    override val descriptor: StubDescriptor
        get() = StubDescriptor.Object
    override val name: String? = classId?.relativeClassName?.toString()

    override fun getNext(element: PsiElement): PsiElement? = element
        .children
        .asSequence()
        .filterIsInstance<KtObjectDeclaration>()
        .find { create(it) == this }

    override fun hashCode(): Int {
        return Objects.hash(descriptor, name)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is KtObjectStub) return false
        return name == other.name
    }

    override fun toString(): String = "KtObjectStub(name=$name)"

    companion object {
        fun create(element: KtObjectDeclaration) =
            KtObjectStub(
                element.getClassId(),
            )
    }

}