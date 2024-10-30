package org.plan.research.minimization.plugin.psi

import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

import java.nio.file.Path

import kotlin.io.path.relativeTo

class PsiItemStorage private constructor(private val map: Map<Path, PsiTrie>, private val project: Project) {
    val usedPaths: Set<Path>
        get() = map.keys
    private val rootPath = project.guessProjectDir()!!.toNioPath()

    suspend fun processMarkedElements(rootElement: PsiFile, processor: suspend (PsiElement) -> Unit) {
        val relativePath = rootElement.virtualFile.toNioPath().relativeTo(rootPath)
        val trie = map[relativePath] ?: return
        trie.processMarkedElements(rootElement, processor)
    }

    companion object {
        fun create(
            items: List<PsiWithBodyDDItem>,
            markedElements: Set<PsiWithBodyDDItem>,
            currentProject: Project,
        ): PsiItemStorage {
            val focusedMap = markedElements.groupBy(PsiWithBodyDDItem::localPath)
            val map = items.groupBy(PsiWithBodyDDItem::localPath)
                .mapValues { (key, items) -> PsiTrie.create(items, focusedMap[key]?.toSet() ?: emptySet()) }
            return PsiItemStorage(map.filterValues(PsiTrie::hasMarkedElements), currentProject)
        }
    }
}
