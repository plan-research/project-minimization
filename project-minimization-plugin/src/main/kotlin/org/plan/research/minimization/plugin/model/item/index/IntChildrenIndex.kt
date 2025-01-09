package org.plan.research.minimization.plugin.model.item.index

import com.intellij.psi.PsiElement

class IntChildrenIndex(val childrenIndex: Int) : PsiChildrenPathIndex, Comparable<IntChildrenIndex> {
    override fun getNext(element: PsiElement): PsiElement? = element.children[childrenIndex]
    override fun compareTo(other: IntChildrenIndex): Int = childrenIndex.compareTo(other.childrenIndex)
    override fun equals(other: Any?) = childrenIndex == (other as? IntChildrenIndex)?.childrenIndex
    override fun hashCode() = childrenIndex.hashCode()
    override fun toString() = childrenIndex.toString()
}