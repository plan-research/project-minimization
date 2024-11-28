package org.plan.research.minimization.plugin.lenses

import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.IJDDItem
import org.plan.research.minimization.plugin.model.ProjectItemLens
import org.plan.research.minimization.plugin.model.PsiChildrenPathIndex
import org.plan.research.minimization.plugin.model.PsiDDItem
import org.plan.research.minimization.plugin.psi.PsiUtils
import org.plan.research.minimization.plugin.psi.trie.PsiTrie

import arrow.core.fold
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.vfs.findFile
import com.intellij.psi.PsiElement
import mu.KotlinLogging
import org.jetbrains.kotlin.psi.KtFile

import java.nio.file.Path

/**
 * An abstract class for the PSI element focusing lens
 */
abstract class BasePsiLens<I, T> :
    ProjectItemLens where I : PsiDDItem<T>, T : Comparable<T>, T : PsiChildrenPathIndex {
    private val logger = KotlinLogging.logger {}
    final override suspend fun focusOn(
        items: List<IJDDItem>,
        currentContext: IJDDContext,
    ): IJDDContext {
        val currentLevel = currentContext.currentLevel as? List<I>
        if (currentContext.currentLevel == null || currentLevel == null) {
            logger.warn { "Some item from current level are not PsiWithBodyDDItem. The wrong lens is used. " }
            return currentContext
        }
        logger.info { "Built a trie for the current context" }
        val items = items as? List<I>
        items ?: run {
            logger.warn { "Some items from $items are not PsiDDItem. The wrong lens is used. " }
            return currentContext
        }
        logFocusedItems(items, currentContext)
        val levelDiff = (currentLevel.toSet() - items.toSet()).groupBy(PsiDDItem<T>::localPath)
        val finalContext =
            levelDiff.fold(currentContext) { context, (path, items) -> focusOnInsideFile(context, items, path) }

        logger.info { "Focusing complete" }
        return finalContext
    }

    private suspend fun logFocusedItems(items: List<I>, context: IJDDContext) {
        if (!logger.isTraceEnabled) {
            return
        }
        val psiElements = items.map { PsiUtils.getPsiElementFromItem(context, it) }
        readAction {
            logger.trace {
                "Focusing on items: \n${psiElements.joinToString("\n") { "\t- ${it?.text}" }}"
            }
        }
    }

    protected open suspend fun useTrie(trie: PsiTrie<I, T>, context: IJDDContext, ktFile: KtFile): IJDDContext {
        PsiUtils.performPsiChangesAndSave(context, ktFile) {
            trie.processMarkedElements(ktFile) { item, psiElement -> focusOnPsiElement(item, psiElement, context) }
        }
        return context
    }

    protected abstract fun focusOnPsiElement(item: I, psiElement: PsiElement, context: IJDDContext)
    protected open fun createTrie(items: List<I>, context: IJDDContext): PsiTrie<I, T> =
        PsiTrie.create(items)

    protected abstract fun getWriteCommandActionName(psiFile: KtFile, context: IJDDContext): String

    private suspend fun focusOnInsideFile(
        currentContext: IJDDContext,
        focusItems: List<I>,
        relativePath: Path,
    ): IJDDContext {
        val trie = createTrie(focusItems, currentContext)
        val virtualFile = readAction {
            currentContext.projectDir.findFile(relativePath.toString())
        }
        virtualFile ?: run {
            logger.error { "The desired path for focused path $relativePath is not found in the project (name=${currentContext.indexProject.name})" }
            return currentContext
        }
        val psiFile = smartReadAction(currentContext.indexProject) { PsiUtils.getKtFile(currentContext, virtualFile) }
        psiFile ?: run {
            logger.error { "The desired path for focused path $relativePath is not a Kotlin file in the project (name=${currentContext.indexProject.name})" }
            return currentContext
        }
        logger.trace { "Processing all focused elements in $relativePath" }
        return useTrie(trie, currentContext, psiFile)
    }
}
