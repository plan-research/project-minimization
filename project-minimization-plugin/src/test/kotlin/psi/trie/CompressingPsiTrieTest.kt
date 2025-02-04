package psi.trie

import generateAllPermutations
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.plan.research.minimization.plugin.modification.item.index.IntChildrenIndex
import org.plan.research.minimization.plugin.modification.item.PsiChildrenIndexDDItem
import org.plan.research.minimization.plugin.modification.psi.CompressingPsiItemTrie
import kotlin.io.path.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompressingPsiTrieTest {
    @Test
    fun `test one item`() {
        val item = PsiChildrenIndexDDItem(
            localPath = Path("."),
            childrenPath = listOf(IntChildrenIndex(1)),
            renderedType = null
        )
        val trie = CompressingPsiItemTrie.Companion.create(listOf(item))
        val items = trie.getNextItems()
        assertEquals(1, items.size)
        val (compressedItem) = items
        assertEquals(1, compressedItem.depth)
        assertEquals(item, compressedItem.item)
        assertTrue(compressedItem.node.getNextItems().isEmpty())
    }

    @Test
    fun `different files do not allowed`() {
        val items = listOf(
            PsiChildrenIndexDDItem(
                localPath = Path("a"),
                childrenPath = listOf(IntChildrenIndex(1)),
                renderedType = null
            ),
            PsiChildrenIndexDDItem(
                localPath = Path("b"),
                childrenPath = listOf(IntChildrenIndex(1)),
                renderedType = null
            )
        )
        assertThrows<IllegalArgumentException> { CompressingPsiItemTrie.Companion.create(items) }
    }

    @Test
    fun `test simple tree`() {
        val paths = listOf(
            listOf(0),
            listOf(1),
            listOf(2),
            listOf(0, 0),
            listOf(1, 0),
            listOf(2, 0),
        )
        val items = paths.map {
            PsiChildrenIndexDDItem(
                localPath = Path("."),
                childrenPath = it.map { IntChildrenIndex(it) },
                renderedType = null
            )
        }
        val trie = CompressingPsiItemTrie.Companion.create(items)
        val compressed = trie.getNextItems()
        assertEquals(3, compressed.size)
        val (item1, item2, item3) = compressed
        assertEquals(1, item1.depth)
        assertEquals(1, item2.depth)
        assertEquals(1, item3.depth)
        assertEquals(items.filter { it.childrenPath.size == 1 }.toSet(), setOf(item1.item, item2.item, item3.item))
        val moreCompressed = listOf(item1, item2, item3).flatMap { it.node.getNextItems() }
        assertEquals(3, moreCompressed.size)
        val (item4, item5, item6) = moreCompressed
        assertEquals(2, item4.depth)
        assertEquals(2, item5.depth)
        assertEquals(2, item6.depth)
        assertEquals(items.filter { it.childrenPath.size == 2 }.toSet(), moreCompressed.map { it.item }.toSet())
        val next = moreCompressed.flatMap { it.node.getNextItems() }
        assertTrue(next.isEmpty())
    }

    @Test
    fun `test compression`() {
        fun doTest(permutation: List<PsiChildrenIndexDDItem>) {
            val trie = CompressingPsiItemTrie.Companion.create(permutation)
            val compressed = trie.getNextItems()
            assertEquals(2, compressed.size)
            val depth3 = compressed.filter { it.depth == 3 }
            assertEquals(1, depth3.size)
            val (firstLevelDepth3) = depth3
            assertEquals(listOf(1, 0, 0).map { IntChildrenIndex(it) }, firstLevelDepth3.item.childrenPath)
            assertTrue(firstLevelDepth3.node.getNextItems().isEmpty())
            val depth1 = compressed.first { it.depth == 1 }
            val moreCompressed = depth1.node.getNextItems()
            assertEquals(2, moreCompressed.size)
            val item3 = moreCompressed.first { it.depth == 3 }
            assertEquals(listOf(0, 0, 0).map { IntChildrenIndex(it) }, item3.item.childrenPath)
            val item4 = moreCompressed.first { it.depth == 2 }
            assertEquals(listOf(0, 1).map { IntChildrenIndex(it) }, item4.item.childrenPath)
            val moreMoreCompressed = item4.node.getNextItems()
            assertEquals(1, moreMoreCompressed.size)
            val item5 = moreMoreCompressed.first()
            assertEquals(listOf(0, 1, 0).map { IntChildrenIndex(it) }, item5.item.childrenPath)
            val moreMoreMoreCompressed = item5.node.getNextItems()
            val item6 = moreMoreMoreCompressed.first()
            assertEquals(listOf(0, 1, 0, 0).map { IntChildrenIndex(it) }, item6.item.childrenPath)
            assertTrue(item6.node.getNextItems().isEmpty())
        }

        val paths = listOf(
            listOf(0),
            listOf(0, 0, 0),
            listOf(0, 1),
            listOf(0, 1, 0, 0),
            listOf(0, 1, 0),
            listOf(1, 0, 0)
        )
        val items = paths.map {
            PsiChildrenIndexDDItem(
                localPath = Path("."),
                childrenPath = it.map { IntChildrenIndex(it) },
                renderedType = null
            )
        }
        val possibleIndexes = items.indices.toSet()
        generateAllPermutations(possibleIndexes).forEach { idxs ->
            doTest(idxs.map { items[it] })
        }
    }

    @Test
    fun `test distant nodes`() {
        val paths = listOf(
            listOf(0, 0),
            listOf(0, 1)
        )
        val items = paths.map {
            PsiChildrenIndexDDItem(
                localPath = Path("."),
                childrenPath = it.map { IntChildrenIndex(it) },
                renderedType = null
            )
        }
        val trie = CompressingPsiItemTrie.Companion.create(items)
        val compressed = trie.getNextItems()
        assertEquals(2, compressed.size)
        assert(compressed.all { it.depth == 2})
    }
}