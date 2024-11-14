package org.plan.research.minimization.plugin.psi.trie

import org.plan.research.minimization.plugin.model.PsiChildrenPathIndex
import org.plan.research.minimization.plugin.model.PsiDDItem

import com.intellij.psi.PsiElement
import mu.KotlinLogging
import org.jetbrains.kotlin.utils.addToStdlib.same


typealias PsiProcessor<ITEM> = (ITEM, PsiElement) -> Unit
private typealias AdjacentNodes<I, T> = MutableMap<T, PsiTrie<I, T>>

/**
 * The PsiTrie class represents a trie structure designed to store and process
 * PSI elements associated with specific PSI elements in the root project.
 */
class PsiTrie<I, T> private constructor() where I : PsiDDItem<T>, T : Comparable<T>, T : PsiChildrenPathIndex {
    private val children: AdjacentNodes<I, T> = mutableMapOf()
    private var containingItem: I? = null
    private val logger = KotlinLogging.logger {}

    fun processMarkedElements(element: PsiElement, processor: PsiProcessor<I>) {
        containingItem?.let {
            if (logger.isTraceEnabled) {
                // to preserve suspended context
                logger.trace(
                    "Processing marked element: ${element.textOffset} in ${element.containingFile.virtualFile.path}",
                )
            }
            processor(it, element)
            return
        }
        children.entries.sortedByDescending { it.key }.forEach { (index, childTrie) ->
            val childElement = index.getNext(element)
            childElement ?: run {
                logger.debug { "Can't find children element for $index in $element" }
                return@forEach
            }
            childTrie.processMarkedElements(childElement, processor)
        }
    }

    private fun add(item: I, depth: Int = 0) {
        if (depth == item.childrenPath.size) {
            containingItem = item
            return
        }
        val nextIndex = item.childrenPath[depth]
        val nextTrie = children.getOrPut(nextIndex) { PsiTrie() }
        nextTrie.add(item, depth + 1)
    }

    companion object {
        fun <ITEM, T> create(items: List<ITEM>): PsiTrie<ITEM, T> where ITEM : PsiDDItem<T>, T : Comparable<T>, T : PsiChildrenPathIndex {
            require(items.same(PsiDDItem<T>::localPath))
            val rootNode = PsiTrie<ITEM, T>()
            items.forEach { rootNode.add(it) }
            return rootNode
        }
    }
}
