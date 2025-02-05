package org.plan.research.minimization.plugin.lenses

import org.plan.research.minimization.plugin.model.ProjectItemLens
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.item.PsiDDItem
import org.plan.research.minimization.plugin.model.item.index.PsiChildrenPathIndex
import org.plan.research.minimization.plugin.model.monad.IJDDContextMonad
import org.plan.research.minimization.plugin.psi.PsiUtils
import org.plan.research.minimization.plugin.psi.trie.PsiTrie

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.vfs.findFileOrDirectory
import com.intellij.psi.PsiElement
import mu.KotlinLogging
import org.jetbrains.kotlin.psi.KtFile

import java.nio.file.Path

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

    context(IJDDContextMonad<C>)
    protected open suspend fun useTrie(trie: PsiTrie<I, T>, ktFile: KtFile) {
        PsiUtils.performPsiChangesAndSave(context, ktFile) {
            trie.processMarkedElements(ktFile) { item, psiElement -> focusOnPsiElement(item, psiElement, context) }
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
        val psiFile = smartReadAction(context.indexProject) { PsiUtils.getKtFile(context, virtualFile) }
        psiFile ?: run {
            logger.error { "The desired path for focused path $relativePath is not a Kotlin file in the project (name=${context.indexProject.name})" }
            return
        }
        logger.trace { "Processing all focused elements in $relativePath" }
        useTrie(trie, psiFile)
    }

    protected abstract suspend fun focusOnFilesAndDirectories(itemsToDelete: List<I>, context: C)
}
