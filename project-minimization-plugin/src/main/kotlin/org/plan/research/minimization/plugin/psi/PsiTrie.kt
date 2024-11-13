package org.plan.research.minimization.plugin.psi

import org.plan.research.minimization.plugin.model.PsiChildrenPathDDItem

import com.intellij.psi.PsiElement
import mu.KotlinLogging
import org.jetbrains.kotlin.utils.addToStdlib.same
import org.plan.research.minimization.plugin.model.PsiChildrenPathIndex
import org.plan.research.minimization.plugin.model.PsiDDItem

/**
 * The PsiTrie class represents a trie structure designed to store and process
 * PSI elements associated with specific PSI elements in the root project.
 */
class PsiTrie<ITEM, T> private constructor() where ITEM : PsiDDItem<T>, T : Comparable<T>, T : PsiChildrenPathIndex {
    private val children: MutableMap<T, PsiTrie<ITEM, T>> = mutableMapOf()
    private var isMarked: Boolean = false
    private val logger = KotlinLogging.logger {}
    var hasMarkedElements: Boolean = false
        private set

    fun processMarkedElements(element: PsiElement, processor: (PsiElement) -> Unit) {
        if (isMarked) {
            if (logger.isTraceEnabled) {
                // to preserve suspended context
                logger.trace(
                    "Processing marked element: ${element.textOffset} in ${element.containingFile.virtualFile.path}",
                )
            }
            processor(element)
            return
        }
        for ((index, childTrie) in children.entries.sortedByDescending { it.key }) {
            if (!childTrie.hasMarkedElements) {
                continue
            }
            val childElement = index.getNext(element)
            if (childElement == null) {
                logger.debug { "Can't find children element for $index in $element" }
                continue
            }
            childTrie.processMarkedElements(childElement, processor)
        }
    }

    private fun add(item: ITEM, isFocused: Boolean, depth: Int = 0) {
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
        fun <ITEM, T> create(
            items: List<ITEM>,
            markedElements: Set<ITEM>
        ): PsiTrie<ITEM, T> where ITEM : PsiDDItem<T>, T : Comparable<T>, T : PsiChildrenPathIndex {
            require(items.same(PsiDDItem<T>::localPath))
            val rootNode = PsiTrie<ITEM, T>()
            items.forEach { rootNode.add(it, markedElements.contains(it)) }
            return rootNode
        }
    }
}
