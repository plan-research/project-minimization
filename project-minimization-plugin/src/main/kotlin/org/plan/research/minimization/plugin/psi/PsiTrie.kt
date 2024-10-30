package org.plan.research.minimization.plugin.psi

import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem

import com.intellij.openapi.application.readAction
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.utils.addToStdlib.same

/**
 * A trie for Psi traversing. The implementation is mutable in terms of marking. However, new items could not be added.
 * TODO!!!!!
 */
class PsiTrie private constructor() {
    private val children: MutableMap<Int, PsiTrie> = mutableMapOf()
    private var isMarked: Boolean = false
    var hasMarkedElements: Boolean = false
        private set
    suspend fun processMarkedElements(element: PsiElement, processor: suspend (PsiElement) -> Unit) {
        if (isMarked) {
            processor(element)
            return
        }
        for ((index, childTrie) in children) {
            if (!childTrie.hasMarkedElements) {
                continue
            }
            val childElement = readAction { element.children[index] }
            childTrie.processMarkedElements(childElement, processor)
        }
    }

    private fun add(item: PsiWithBodyDDItem, isFocused: Boolean, depth: Int = 0) {
        if (depth == item.childrenPath.size) {
            this.isMarked = isFocused
            hasMarkedElements = isFocused
            return
        }
        val nextIndex = item.childrenPath[depth]
        val nextTrie = children.getOrPut(nextIndex) { PsiTrie() }
        nextTrie.add(item, isFocused, depth + 1)
        hasMarkedElements = hasMarkedElements || nextTrie.hasMarkedElements
    }

    companion object {
        fun create(items: List<PsiWithBodyDDItem>, markedElements: Set<PsiWithBodyDDItem>): PsiTrie {
            require(items.same(PsiWithBodyDDItem::localPath))
            val rootNode = PsiTrie()
            items.forEach { rootNode.add(it, markedElements.contains(it)) }
            return rootNode
        }
    }
}
