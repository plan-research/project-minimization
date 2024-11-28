package org.plan.research.minimization.plugin.psi

import org.plan.research.minimization.plugin.model.PsiChildrenPathIndex
import org.plan.research.minimization.plugin.model.PsiDDItem
import org.plan.research.minimization.plugin.model.PsiStubDDItem
import org.plan.research.minimization.plugin.psi.CompressingPsiItemTrie.NextPsiDDItemInfo
import org.plan.research.minimization.plugin.psi.stub.KtStub

import mu.KotlinLogging
import org.jetbrains.kotlin.utils.addToStdlib.same

typealias StubCompressingPsiTrie = CompressingPsiItemTrie<PsiStubDDItem, KtStub>
private typealias AdjacentNodes<I, T> = MutableMap<T, CompressingPsiItemTrie<I, T>>
private typealias NextAdjacentNodes<I, T> = MutableMap<T, NextPsiDDItemInfo<I, T>>

/**
 * The Trie that is designed to compress the children's path of the PSI item to quickly lookup for the child PSI DD element in the PSI tree
 */
class CompressingPsiItemTrie<I, T> private constructor() where I : PsiDDItem<T>, T : PsiChildrenPathIndex {
    private val children: AdjacentNodes<I, T> = mutableMapOf()
    private val logger = KotlinLogging.logger {}
    private val closestPsiItems: NextAdjacentNodes<I, T> = mutableMapOf()
    var maxDepth: Int = 0
        private set
    private var correspondingItem: I? = null

    /**
     * Retrieves the list of next PsiDDItems available from the current node in the trie structure.

     *
     * @return A list containing the next items in the trie.
     */
    fun getNextItems() = closestPsiItems.values.toList()

    private fun add(item: I, depth: Int = 0): NextPsiDDItemInfo<I, T> {
        if (depth == item.childrenPath.size) {
            require(correspondingItem == null)
            correspondingItem = item
            maxDepth = maxDepth.coerceAtLeast(depth)
            return NextPsiDDItemInfo(this, item, depth)
        }
        val edge = item.childrenPath[depth]
        val nextNode = children.getOrPut(edge) { CompressingPsiItemTrie() }
        val addedNode = nextNode.add(item, depth + 1)
        closestPsiItems.compute(edge) { _, previousValue ->
            if (previousValue == null || previousValue.depth > addedNode.depth) {
                addedNode
            } else {
                previousValue
            }
        }
        maxDepth = maxDepth.coerceAtLeast(nextNode.maxDepth)
        return addedNode
    }

    data class NextPsiDDItemInfo<I, T : PsiChildrenPathIndex>(
        val node: CompressingPsiItemTrie<I, T>,
        val item: I,
        val depth: Int,
    ) where I : PsiDDItem<T>

    companion object {
        fun <ITEM, T : PsiChildrenPathIndex> create(items: List<ITEM>): CompressingPsiItemTrie<ITEM, T> where ITEM : PsiDDItem<T> {
            require(items.same(PsiDDItem<T>::localPath))
            val rootNode = CompressingPsiItemTrie<ITEM, T>()
            items.forEach { rootNode.add(it) }
            return rootNode
        }
    }
}
