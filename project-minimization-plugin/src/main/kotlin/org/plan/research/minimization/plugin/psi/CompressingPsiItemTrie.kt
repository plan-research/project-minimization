package org.plan.research.minimization.plugin.psi

import org.plan.research.minimization.plugin.model.PsiDDItem

import mu.KotlinLogging
import org.jetbrains.kotlin.utils.addToStdlib.getOrPut
import org.jetbrains.kotlin.utils.addToStdlib.same

/**
 * The Trie that is designed to compress the children's path of the PSI item to quickly lookup for the child PSI DD element in the PSI tree
 */
class CompressingPsiItemTrie private constructor() {
    private val children: MutableMap<Int, CompressingPsiItemTrie> = mutableMapOf()
    private val logger = KotlinLogging.logger {}
    private val nextItem: MutableMap<Int, NextPsiDDItemInfo> = mutableMapOf()
    private var correspondingItem: PsiDDItem? = null

    /**
     * Retrieves the list of next PsiDDItems available from the current node in the trie structure.

     *
     * @return A list containing the next items in the trie.
     */
    fun getNextItems() = nextItem.values.toList()

    private fun add(item: PsiDDItem, depth: Int = 0): NextPsiDDItemInfo {
        if (depth == item.childrenPath.size) {
            require(correspondingItem == null)
            correspondingItem = item
            return NextPsiDDItemInfo(this, item, depth)
        }
        val edge = item.childrenPath[depth]
        val nextNode = children.getOrPut(edge) { CompressingPsiItemTrie() }
        val addedNode = nextNode.add(item, depth + 1)
        nextItem.compute(edge) { _, previousValue ->
            if (previousValue == null || previousValue.depth > addedNode.depth) {
                addedNode
            } else {
                previousValue
            }
        }
        return addedNode
    }

    data class NextPsiDDItemInfo(val node: CompressingPsiItemTrie, val item: PsiDDItem, val depth: Int)

    companion object {
        fun create(items: List<PsiDDItem>): CompressingPsiItemTrie {
            require(items.same(PsiDDItem::localPath))
            val rootNode = CompressingPsiItemTrie()
            items.forEach { rootNode.add(it) }
            return rootNode
        }
    }
}
