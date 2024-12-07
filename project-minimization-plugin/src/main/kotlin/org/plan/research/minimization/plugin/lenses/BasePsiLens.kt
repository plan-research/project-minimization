package org.plan.research.minimization.plugin.lenses

import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.IJDDItem
import org.plan.research.minimization.plugin.model.ProjectItemLens
import org.plan.research.minimization.plugin.model.PsiChildrenPathIndex
import org.plan.research.minimization.plugin.model.PsiDDItem
import org.plan.research.minimization.plugin.psi.PsiUtils
import org.plan.research.minimization.plugin.psi.trie.PsiItemStorage

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
abstract class BasePsiLens<I, T> : ProjectItemLens where I : PsiDDItem<T>, T : Comparable<T>, T : PsiChildrenPathIndex {
    private val logger = KotlinLogging.logger {}
    final override suspend fun focusOn(
        items: List<IJDDItem>,
        currentContext: IJDDContext,
    ) {
        val currentLevel = currentContext.currentLevel as? List<I>
        if (currentContext.currentLevel == null || currentLevel == null) {
            logger.warn { "Some item from current level are not PsiWithBodyDDItem. The wrong lens is used. " }
            return
        }
        logger.info { "Built a trie for the current context" }
        val items = items as? List<I>
        items ?: run {
            logger.warn { "Some items from $items are not PsiDDItem. The wrong lens is used. " }
            return
        }

        logFocusedItems(items, currentContext)
        val currentLevelTrie = PsiItemStorage.create(
            currentLevel.toSet() - items.toSet(),
            currentContext,
        )

        currentLevelTrie.usedPaths.forEach { focusOnInsideFile(currentContext, currentLevelTrie, it) }
        logger.info { "Focusing complete" }
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

    protected abstract fun focusOnPsiElement(item: I, psiElement: PsiElement, context: IJDDContext)
    protected abstract fun getWriteCommandActionName(psiFile: KtFile, context: IJDDContext): String
    protected open suspend fun postProcessPsiFile(psiFile: KtFile, context: IJDDContext) = Unit

    private suspend fun focusOnInsideFile(currentContext: IJDDContext, trie: PsiItemStorage<I, T>, relativePath: Path) {
        val virtualFile = readAction {
            currentContext.projectDir.findFile(relativePath.toString())
        }
        virtualFile ?: run {
            logger.error { "The desired path for focused path $relativePath is not found in the project (name=${currentContext.indexProject.name})" }
            return
        }
        val psiFile = smartReadAction(currentContext.indexProject) {
            PsiUtils.getKtFile(currentContext, virtualFile)
        }
        psiFile ?: run {
            logger.error { "The desired path for focused path $relativePath is not a Kotlin file in the project (name=${currentContext.indexProject.name})" }
            return
        }
        logger.debug { "Processing all focused elements in $relativePath" }
        PsiUtils.performPsiChangesAndSave(currentContext, psiFile) {
            trie.processMarkedElements(psiFile) { item, psiElement -> focusOnPsiElement(item, psiElement, currentContext) }
        }
        postProcessPsiFile(psiFile, currentContext)
    }
}
