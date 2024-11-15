package org.plan.research.minimization.plugin.psi

import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem

import com.intellij.psi.PsiElement
import mu.KotlinLogging
import org.jetbrains.kotlin.utils.addToStdlib.same

typealias PsiProcessor = (PsiWithBodyDDItem, PsiElement) -> Unit

/**
 * The PsiTrie class represents a trie structure designed to store and process
 * PSI elements associated with specific PSI elements in the root project.
 */
class PsiTrie private constructor() {
    private val children: MutableMap<Int, PsiTrie> = mutableMapOf()
    private var containingItem: PsiWithBodyDDItem? = null
    private val logger = KotlinLogging.logger {}

    fun processMarkedElements(element: PsiElement, processor: PsiProcessor) {
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
        for ((index, childTrie) in children) {
            val childElement = element.children[index]
            childTrie.processMarkedElements(childElement, processor)
        }
    }

    private fun add(item: PsiWithBodyDDItem, depth: Int = 0) {
        if (depth == item.childrenPath.size) {
            containingItem = item
            return
        }
        val nextIndex = item.childrenPath[depth]
        val nextTrie = children.getOrPut(nextIndex) { PsiTrie() }
        nextTrie.add(item, depth + 1)
    }

    companion object {
        fun create(items: List<PsiWithBodyDDItem>): PsiTrie {
            require(items.same(PsiWithBodyDDItem::localPath))
            val rootNode = PsiTrie()
            items.forEach { rootNode.add(it) }
            return rootNode
        }
    }
}
