package org.plan.research.minimization.plugin.lenses

import org.plan.research.minimization.plugin.model.IJDDContext
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
    ProjectItemLens<I> where I : PsiDDItem<T>, T : Comparable<T>, T : PsiChildrenPathIndex {
    private val logger = KotlinLogging.logger {}
    final override suspend fun focusOn(
        items: List<I>,
        currentContext: IJDDContext,
    ): IJDDContext {
        val currentLevel = currentLevel(currentContext) ?: run {
            logger.warn { "Some item from current level are not PsiWithBodyDDItem. The wrong lens is used. " }
            return currentContext
        }
        val currentContext = prepareContext(currentContext, items) ?: run {
            logger.error { "Can't prepare the context. Giving up focusing." }
            return currentContext
        }
        logger.info { "Built a trie for the current context" }
        val items = items
        logFocusedItems(items, currentContext)
        val levelDiff = (currentLevel.toSet() - items.toSet())
            .flatMap { transformSelectedElements(it, currentContext) }
            .groupBy(PsiDDItem<T>::localPath)
        val finalContext =
            levelDiff.fold(currentContext) { context, (path, items) -> focusOnInsideFile(context, items, path) }

        logger.info { "Focusing complete" }
        return finalContext
    }

    protected open fun transformSelectedElements(item: I, context: IJDDContext): List<I> = listOf(item)
    protected open fun prepareContext(context: IJDDContext, items: List<I>): IJDDContext? = context

    private suspend fun logFocusedItems(items: List<I>, context: IJDDContext) {
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

    protected open suspend fun useTrie(trie: PsiTrie<I, T>, context: IJDDContext, ktFile: KtFile): IJDDContext {
        PsiUtils.performPsiChangesAndSave(context, ktFile) {
            trie.processMarkedElements(ktFile) { item, psiElement -> focusOnPsiElement(item, psiElement, context) }
        }
        return context
    }

    protected abstract fun focusOnPsiElement(item: I, psiElement: PsiElement, context: IJDDContext)

    protected abstract fun getWriteCommandActionName(psiFile: KtFile, context: IJDDContext): String

    private suspend fun focusOnInsideFile(
        currentContext: IJDDContext,
        focusItems: List<I>,
        relativePath: Path,
    ): IJDDContext {
        val trie = PsiTrie.create(focusItems)
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

    protected abstract fun currentLevel(context: IJDDContext): List<I>?
}
