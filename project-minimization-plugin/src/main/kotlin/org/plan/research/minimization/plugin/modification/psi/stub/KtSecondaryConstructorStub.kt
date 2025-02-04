package org.plan.research.minimization.plugin.modification.psi.stub

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import java.util.Objects

data class KtSecondaryConstructorStub(
    override val name: String?,
    val parameterList: List<KtParameterStub>,
) : KtStub() {
    override val descriptor: StubDescriptor
        get() = StubDescriptor.SECONDARY_CONSTRUCTOR

    override fun equals(other: Any?): Boolean {
        if (other !is KtSecondaryConstructorStub) {
            return false
        }
        return name == other.name && parameterList == other.parameterList
    }

    override fun hashCode(): Int = Objects.hash(descriptor, name, parameterList)

    override fun toString(): String =
        "KtSecondaryConstructorStub(name=$name, parameterList=$parameterList)"

    override fun getNext(element: PsiElement): PsiElement? = element
        .children
        .asSequence()
        .filterIsInstance<KtSecondaryConstructor>()
        .find { create(it) == this }

    companion object {
        fun create(element: KtSecondaryConstructor): KtSecondaryConstructorStub =
            KtSecondaryConstructorStub(
                element.name,
                element.valueParameters.map(KtParameterStub::create),
            )
    }
}
