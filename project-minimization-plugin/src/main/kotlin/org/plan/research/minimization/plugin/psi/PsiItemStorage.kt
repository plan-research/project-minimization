package org.plan.research.minimization.plugin.psi

import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.PsiChildrenPathIndex
import org.plan.research.minimization.plugin.model.PsiDDItem

import com.intellij.psi.PsiFile

import java.nio.file.Path

import kotlin.io.path.relativeTo

private typealias TrieHolder<I, T> = Map<Path, PsiTrie<I, T>>

/**
 * A per-file storage for [PsiTrie]
 */
class PsiItemStorage<I, T> private constructor(
    private val map: TrieHolder<I, T>,
    context: IJDDContext,
) where I : PsiDDItem<T>, T : PsiChildrenPathIndex, T : Comparable<T> {
    val usedPaths: Set<Path>
        get() = map.keys
    private val rootPath = context.projectDir.toNioPath()

    fun processMarkedElements(rootElement: PsiFile, processor: PsiProcessor<I>) {
        val relativePath = rootElement.virtualFile.toNioPath().relativeTo(rootPath)
        val trie = map[relativePath] ?: return
        trie.processMarkedElements(rootElement, processor)
    }

    companion object {
        fun<ITEM, T> create(
            markedElements: Set<ITEM>,
            context: IJDDContext,
        ): PsiItemStorage<ITEM, T> where ITEM : PsiDDItem<T>, T : PsiChildrenPathIndex, T : Comparable<T> {
            val focusedMap = markedElements.groupBy(PsiDDItem<T>::localPath)
                .filter { it.value.isNotEmpty() }
                .mapValues { PsiTrie.create(it.value) }
            return PsiItemStorage(focusedMap, context)
        }
    }
}
