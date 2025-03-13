package org.plan.research.minimization.plugin.modification.lenses

import org.plan.research.minimization.plugin.context.IJDDContext
import org.plan.research.minimization.plugin.context.IJDDContextMonad
import org.plan.research.minimization.plugin.modification.item.PsiDDItem
import org.plan.research.minimization.plugin.modification.item.index.PsiChildrenPathIndex
import org.plan.research.minimization.plugin.modification.psi.PsiUtils
import org.plan.research.minimization.plugin.modification.psi.trie.PsiTrie

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.vfs.findFileOrDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.concurrency.annotations.RequiresReadLock
import mu.KotlinLogging
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtFile

import java.nio.file.Path

import kotlin.io.path.relativeTo

/**
 * An abstract class for the PSI element focusing lens
 */
abstract class BasePsiLens<C, I, T> :
    ProjectItemLens<C, I> where C : IJDDContext, I : PsiDDItem<T>, T : Comparable<T>, T : PsiChildrenPathIndex {
    private val logger = KotlinLogging.logger {}

    context(IJDDContextMonad<C>)
    final override suspend fun focusOn(
        itemsToDelete: List<I>,
    ) {
        logger.info { "Built a trie for the current context" }
        logFocusedItems(itemsToDelete, context)
        prepare(itemsToDelete)
        val (filesAndDirectories, otherPsiObjects) = itemsToDelete.partition { it.childrenPath.isEmpty() }
        val levelDiff = otherPsiObjects
            .flatMap { transformSelectedElements(it, context) }
            .groupBy(PsiDDItem<T>::localPath)

        focusOnFilesAndDirectories(filesAndDirectories, context)
        levelDiff.forEach { (path, items) -> focusOnInsideFile(items, path) }

        logger.info { "Focusing complete" }
    }

    protected open fun transformSelectedElements(item: I, context: C): List<I> = listOf(item)

    context(IJDDContextMonad<C>)
    protected open suspend fun prepare(itemsToDelete: List<I>) = Unit

    private suspend fun logFocusedItems(items: List<I>, context: C) {
        if (!logger.isTraceEnabled) {
            return
        }
        readAction {
            val psiElements = items.map { PsiUtils.getPsiElementFromItem(context, it) }
            logger.trace {
                "Focusing on items: \n${psiElements.joinToString("\n") { "\t- ${it?.text}" }}"
            }
        }
    }

    /**
     * Focus on elements represented by the [PsiTrie] inside the [PsiFile].
     *
     * @param trie The PsiTrie containing items to be focused on.
     * @param file The PsiFile where the changes will be performed and saved.
     */
    context(IJDDContextMonad<C>)
    protected open suspend fun focusOnTrieAndSave(trie: PsiTrie<I, T>, file: PsiFile) {
        PsiUtils.performPsiChangesAndSave(context, file) {
            trie.processMarkedElements(file) { item, psiElement -> focusOnPsiElement(item, psiElement, context) }
        }
    }

    protected abstract fun focusOnPsiElement(item: I, psiElement: PsiElement, context: C)

    context(IJDDContextMonad<C>)
    private suspend fun focusOnInsideFile(
        focusItems: List<I>,
        relativePath: Path,
    ) {
        val trie = PsiTrie.create(focusItems)
        val virtualFile = readAction {
            context.projectDir.findFileOrDirectory(relativePath.toString())
        }
        virtualFile ?: run {
            logger.error { "The desired path for focused path $relativePath is not found in the project (name=${context.indexProject.name})" }
            return
        }
        val psiFile = smartReadAction(context.indexProject) { PsiUtils.getSourceFile(context, virtualFile) }
        psiFile ?: run {
            logger.error { "The desired path for focused path $relativePath is not a source file in the project (name=${context.indexProject.name})" }
            return
        }
        logger.trace { "Processing all focused elements in $relativePath" }
        focusOnTrieAndSave(trie, psiFile)
    }

    protected abstract suspend fun focusOnFilesAndDirectories(itemsToDelete: List<I>, context: C)

    protected fun KtFile.getLocalPath(context: C): Path {
        val rootPath = context.projectDir.toNioPath()
        return this.virtualFile.toNioPath().relativeTo(rootPath)
    }

    @RequiresReadLock
    protected fun IJDDContext.getKtFileInIndexProject(ktFile: KtFile): KtFile? {
        val rootPath = projectDir.toNioPath()
        val localPath = ktFile.virtualFile.toNioPath().relativeTo(rootPath)
        val indexFile = indexProjectDir.findFileByRelativePath(localPath.toString()) ?: run {
            logger.error { "Can't find a local file with path $localPath in index project" }
            return null
        }
        val indexKtFile = indexFile.toPsiFile(indexProject) ?: run {
            logger.error { "Can't find a PSI file for a local file with path $localPath in index project" }
            return null
        }
        return indexKtFile as? KtFile ?: run {
            logger.error { "KtFile with localPath=$localPath is not a KtFile in index project" }
            null
        }
    }
}
