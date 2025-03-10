package org.plan.research.minimization.plugin.modification.item.index

import com.intellij.psi.PsiElement

interface PsiChildrenPathIndex {
    fun getNext(element: PsiElement): PsiElement?
}
