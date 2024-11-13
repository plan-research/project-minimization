package org.plan.research.minimization.plugin.model.psi

import arrow.core.None
import arrow.core.raise.option
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.plan.research.minimization.plugin.model.PsiChildrenPathIndex

abstract class KtStub: PsiChildrenPathIndex, Comparable<KtStub> {
    abstract val name: String?
    abstract val descriptor: StubDescriptor

    companion object {
        fun create(element: PsiElement) = option {
            when (element) {
                is KtNamedFunction -> KtFunctionStub.create(element)
                is KtProperty -> KtPropertyStub.create(element)
                is KtClass -> KtClassStub.create(element)
                is KtClassBody -> KtClassBodyStub
                is KtBlockExpression -> KtBlockExpressionStub
                is KtObjectDeclaration -> KtObjectStub.create(element)
                else -> raise(None)
            }
        }
    }

    override fun compareTo(other: KtStub): Int {
        val descriptorCompare = descriptor.compareTo(other.descriptor)
        if (descriptorCompare != 0) return descriptorCompare
        return when (name) {
            null -> if (other.name == null) 0 else -1
            else -> if (other.name == null) 1 else name!!.compareTo(other.name!!)
        }
    }
}