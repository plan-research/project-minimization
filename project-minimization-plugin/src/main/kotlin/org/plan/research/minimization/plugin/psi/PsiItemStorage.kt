package org.plan.research.minimization.plugin.psi

import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

import java.nio.file.Path

import kotlin.io.path.relativeTo

/**
 * A per-file storage for [PsiTrie]
 */
class PsiItemStorage private constructor(private val map: Map<Path, PsiTrie>, context: IJDDContext) {
    val usedPaths: Set<Path>
        get() = map.keys
    private val rootPath = context.projectDir.toNioPath()

    fun processMarkedElements(rootElement: PsiFile, processor: (PsiElement) -> Unit) {
        val relativePath = rootElement.virtualFile.toNioPath().relativeTo(rootPath)
        val trie = map[relativePath] ?: return
        trie.processMarkedElements(rootElement, processor)
    }

    companion object {
        fun create(
            items: List<PsiWithBodyDDItem>,
            markedElements: Set<PsiWithBodyDDItem>,
            context: IJDDContext,
        ): PsiItemStorage {
            val focusedMap = markedElements.groupBy(PsiWithBodyDDItem::localPath)
            val map = items.groupBy(PsiWithBodyDDItem::localPath)
                .mapValues { (key, items) -> PsiTrie.create(items, focusedMap[key]?.toSet() ?: emptySet()) }
            return PsiItemStorage(map.filterValues(PsiTrie::hasMarkedElements), context)
        }
    }
}
