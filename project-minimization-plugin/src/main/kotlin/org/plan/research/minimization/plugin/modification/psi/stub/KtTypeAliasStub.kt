package org.plan.research.minimization.plugin.modification.psi.stub

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtTypeAlias
import java.util.Objects

class KtTypeAliasStub(override val name: String?, val typeParams: String) : KtStub() {
    override val descriptor: StubDescriptor
        get() = StubDescriptor.TYPE_ALIAS

    override fun getNext(element: PsiElement): PsiElement? = element
        .children
        .asSequence()
        .filterIsInstance<KtTypeAlias>()
        .find { create(it) == this }

    override fun equals(other: Any?): Boolean {
        if (other !is KtTypeAliasStub) {
            return false
        }
        return name == other.name && typeParams == other.typeParams
    }

    override fun hashCode(): Int = Objects.hash(descriptor, name, typeParams)

    override fun toString(): String =
        "KtTypeAliasStub(name=$name, typeParams=$typeParams)"

    companion object {
        fun create(element: KtTypeAlias) = KtTypeAliasStub(
            element.name,
            element.typeParameterList?.text ?: "",
        )
    }
}
