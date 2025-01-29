package org.plan.research.minimization.plugin.model.item.index

import org.plan.research.minimization.plugin.psi.stub.KtStub

import arrow.core.raise.option
import com.intellij.psi.PsiElement
import fleet.util.indexOfOrNull
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction

import kotlin.collections.filter

sealed class InstructionLookupIndex : PsiChildrenPathIndex, Comparable<InstructionLookupIndex> {
    data class ChildrenNonDeclarationIndex(val childrenIndex: Int) : InstructionLookupIndex() {
        override fun getNext(element: PsiElement): PsiElement? =
            element.children.filter { it !is KtClass && it !is KtNamedFunction }.getOrNull(childrenIndex)

        override fun compareTo(other: InstructionLookupIndex): Int = when (other) {
            is ChildrenNonDeclarationIndex -> childrenIndex.compareTo(other.childrenIndex)
            is StubDeclarationIndex -> 1
        }

        companion object {
            fun create(parent: PsiElement, child: PsiElement) = option {
                ChildrenNonDeclarationIndex(
                    ensureNotNull(
                        parent.children.filter { it !is KtClass && it !is KtNamedFunction }.indexOfOrNull(child),
                    ),
                )
            }
        }
    }

    data class StubDeclarationIndex(val stub: KtStub) : InstructionLookupIndex(), PsiChildrenPathIndex by stub {
        override fun compareTo(other: InstructionLookupIndex): Int = when (other) {
            is StubDeclarationIndex -> stub.compareTo(other.stub)
            is ChildrenNonDeclarationIndex -> -1
        }
    }
}
