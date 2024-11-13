package org.plan.research.minimization.plugin.psi

import mu.KotlinLogging
import org.jetbrains.kotlin.utils.addToStdlib.same
import org.plan.research.minimization.plugin.model.PsiChildrenPathIndex
import org.plan.research.minimization.plugin.model.PsiDDItem
import org.plan.research.minimization.plugin.model.PsiStubDDItem
import org.plan.research.minimization.plugin.model.psi.KtStub

typealias StubCompressingPsiTrie = CompressingPsiItemTrie<PsiStubDDItem, KtStub>

/**
 * The Trie that is designed to compress the children's path of the PSI item to quickly lookup for the child PSI DD element in the PSI tree
 */
class CompressingPsiItemTrie<ITEM, T : PsiChildrenPathIndex> private constructor() where ITEM : PsiDDItem<T> {
    private val children: MutableMap<T, CompressingPsiItemTrie<ITEM, T>> = mutableMapOf()
    private val logger = KotlinLogging.logger {}
    private val nextItem: MutableMap<T, NextPsiDDItemInfo<ITEM, T>> = mutableMapOf()
    private var correspondingItem: ITEM? = null

    /**
     * Retrieves the list of next PsiDDItems available from the current node in the trie structure.

     *
     * @return A list containing the next items in the trie.
     */
    fun getNextItems() = nextItem.values.toList()

    private fun add(item: ITEM, depth: Int = 0): NextPsiDDItemInfo<ITEM, T> {
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

    data class NextPsiDDItemInfo<ITEM, T : PsiChildrenPathIndex>(
        val node: CompressingPsiItemTrie<ITEM, T>,
        val item: ITEM,
        val depth: Int
    ) where ITEM : PsiDDItem<T>

    companion object {
        fun <ITEM, T : PsiChildrenPathIndex> create(items: List<ITEM>): CompressingPsiItemTrie<ITEM, T> where ITEM : PsiDDItem<T> {
            require(items.same(PsiDDItem<T>::localPath))
            val rootNode = CompressingPsiItemTrie<ITEM, T>()
            items.forEach { rootNode.add(it) }
            return rootNode
        }
    }
}
