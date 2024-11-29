package org.plan.research.minimization.plugin.psi.stub

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import java.util.Objects

data class KtFunctionStub(
    override val name: String?,
    val parameterList: List<KtParameterStub>,
) : KtStub() {
    override val descriptor: StubDescriptor
        get() = StubDescriptor.FUNCTION

    override fun equals(other: Any?): Boolean {
        if (other !is KtFunctionStub) {
            return false
        }
        return name == other.name && parameterList == other.parameterList
    }

    override fun hashCode(): Int = Objects.hash(descriptor, name, parameterList)

    override fun toString(): String = "KtFunctionStub(name=$name, parameterList=$parameterList)"

    override fun getNext(element: PsiElement): PsiElement? = element
        .children
        .asSequence()
        .filterIsInstance<KtNamedFunction>()
        .find { create(it) == this }
    companion object {
        fun create(element: KtNamedFunction): KtFunctionStub =
            KtFunctionStub(
                element.name,
                element.valueParameters.map(KtParameterStub::create),
            )
    }
}
