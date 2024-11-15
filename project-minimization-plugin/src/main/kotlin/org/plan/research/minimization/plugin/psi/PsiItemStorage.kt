package org.plan.research.minimization.plugin.psi

import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem

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

    fun processMarkedElements(rootElement: PsiFile, processor: PsiProcessor) {
        val relativePath = rootElement.virtualFile.toNioPath().relativeTo(rootPath)
        val trie = map[relativePath] ?: return
        trie.processMarkedElements(rootElement, processor)
    }

    companion object {
        fun create(
            markedElements: Iterable<PsiWithBodyDDItem>,
            context: IJDDContext,
        ): PsiItemStorage {
            val focusedMap = markedElements.groupBy(PsiWithBodyDDItem::localPath)
                .filter { it.value.isNotEmpty() }
                .mapValues { PsiTrie.create(it.value) }
            return PsiItemStorage(focusedMap, context)
        }
    }
}
