package org.plan.research.minimization.plugin.model.psi

import org.plan.research.minimization.plugin.model.PsiChildrenPathIndex

import arrow.core.None
import arrow.core.raise.option
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty

abstract class KtStub : PsiChildrenPathIndex, Comparable<KtStub> {
    abstract val name: String?
    abstract val descriptor: StubDescriptor

    override fun compareTo(other: KtStub): Int {
        val descriptorCompare = descriptor.compareTo(other.descriptor)
        if (descriptorCompare != 0) {
            return descriptorCompare
        }
        return when (name) {
            null -> other.name?.let {
                -1
            } ?: 0
            else -> other.name?.let {
                name!!.compareTo(other.name!!)
            } ?: 1
        }
    }

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
}
