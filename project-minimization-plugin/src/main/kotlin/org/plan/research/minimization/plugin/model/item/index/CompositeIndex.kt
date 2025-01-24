package org.plan.research.minimization.plugin.model.item.index

import org.plan.research.minimization.plugin.psi.stub.KtStub

import arrow.core.raise.option
import com.intellij.psi.PsiElement
import fleet.util.indexOfOrNull
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction

import kotlin.collections.filter

sealed class CompositeIndex : PsiChildrenPathIndex {
    data class ChildrenNonDeclarationIndex(val childrenIndex: Int) : CompositeIndex() {
        override fun getNext(element: PsiElement): PsiElement? =
            element.children.filter { it !is KtClass && it !is KtNamedFunction }.getOrNull(childrenIndex)

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

    data class StubDeclarationIndex(val stub: KtStub) : CompositeIndex(), PsiChildrenPathIndex by stub
}
