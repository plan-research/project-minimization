package org.plan.research.minimization.plugin.model.item.index

import com.intellij.psi.PsiElement

interface PsiChildrenPathIndex {
    fun getNext(element: PsiElement): PsiElement?
}