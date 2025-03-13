package org.plan.research.minimization.plugin.modification.psi.trie

import org.plan.research.minimization.plugin.modification.item.PsiDDItem
import org.plan.research.minimization.plugin.modification.item.index.PsiChildrenPathIndex

import com.intellij.psi.PsiElement
import mu.KotlinLogging
import org.jetbrains.kotlin.utils.addToStdlib.same

typealias PsiProcessor<ITEM> = (ITEM, PsiElement) -> Unit
private typealias AdjacentNodes<I, T> = MutableMap<T, PsiTrie<I, T>>

/**
 * The [PsiTrie] represents a trie structure designed to store and process
 * PSI elements associated with specific PSI elements in the root project.
 *
 * @param I type of element stored in the trie.
 * @param T type of index stored in edges of the trie.
 */
class PsiTrie<I, T> private constructor()
where I : PsiDDItem<T>,
T : Comparable<T>, T : PsiChildrenPathIndex {
    private val children: AdjacentNodes<I, T> = mutableMapOf()
    private var containingItem: I? = null
    private val logger = KotlinLogging.logger {}

    fun processMarkedElements(element: PsiElement, processor: PsiProcessor<I>) {
        containingItem?.let {
            logger.trace {
                "Processing marked element: ${element.textRange} (type: ${element.javaClass.simpleName}) in ${element.containingFile.virtualFile.path}"
            }
            processor(it, element)
            return
        }
        children.entries.sortedByDescending { it.key }.forEach { (index, childTrie) ->
            val childElement = index.getNext(element)
            childElement ?: run {
                logger.info { "Can't find children element for $index in ${element.javaClass.simpleName}" }
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
        /**
         * Creates a [PsiTrie] from a list of [PsiDDItem]s.
         *
         * @param items list of items. `.localPath` should be the same for all.
         * @return [PsiTrie] constructed from items.
         */
        fun <ITEM, T> create(items: List<ITEM>): PsiTrie<ITEM, T>
        where ITEM : PsiDDItem<T>,
        T : Comparable<T>, T : PsiChildrenPathIndex {
            require(items.same(PsiDDItem<T>::localPath))
            val rootNode = PsiTrie<ITEM, T>()
            items.forEach { rootNode.add(it) }
            return rootNode
        }
    }
}
