package org.plan.research.minimization.plugin.psi

import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.PsiChildrenPathDDItem

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.plan.research.minimization.plugin.model.PsiChildrenPathIndex
import org.plan.research.minimization.plugin.model.PsiDDItem

import java.nio.file.Path

import kotlin.io.path.relativeTo

/**
 * A per-file storage for [PsiTrie]
 */
class PsiItemStorage<ITEM, T> private constructor(
    private val map: Map<Path, PsiTrie<ITEM, T>>,
    context: IJDDContext
) where ITEM : PsiDDItem<T>, T : PsiChildrenPathIndex, T : Comparable<T> {
    val usedPaths: Set<Path>
        get() = map.keys
    private val rootPath = context.projectDir.toNioPath()

    fun processMarkedElements(rootElement: PsiFile, processor: (PsiElement) -> Unit) {
        val relativePath = rootElement.virtualFile.toNioPath().relativeTo(rootPath)
        val trie = map[relativePath] ?: return
        trie.processMarkedElements(rootElement, processor)
    }

    companion object {
        fun<ITEM, T> create(
            items: List<ITEM>,
            markedElements: Set<ITEM>,
            context: IJDDContext,
        ): PsiItemStorage<ITEM, T> where ITEM : PsiDDItem<T>, T : PsiChildrenPathIndex, T : Comparable<T>{
            val focusedMap = markedElements.groupBy(PsiDDItem<T>::localPath)
            val map = items.groupBy(PsiDDItem<T>::localPath)
                .mapValues { (key, items) -> PsiTrie.create(items, focusedMap[key]?.toSet() ?: emptySet()) }
            return PsiItemStorage(map.filterValues(PsiTrie<ITEM, T>::hasMarkedElements), context)
        }
    }
}
