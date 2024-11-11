package org.plan.research.minimization.plugin.lenses

import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.IJDDItem
import org.plan.research.minimization.plugin.model.ProjectItemLens
import org.plan.research.minimization.plugin.model.PsiDDItem
import org.plan.research.minimization.plugin.psi.PsiItemStorage
import org.plan.research.minimization.plugin.psi.PsiUtils
import org.plan.research.minimization.plugin.services.MinimizationPsiManagerService

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.findFile
import com.intellij.psi.PsiElement
import mu.KotlinLogging
import org.jetbrains.kotlin.psi.KtFile

import java.nio.file.Path

/**
 * An abstract class for the PSI element focusing lens
 */
abstract class BasePsiLens : ProjectItemLens {
    private val logger = KotlinLogging.logger {}
    override suspend fun focusOn(
        items: List<IJDDItem>,
        currentContext: IJDDContext,
    ) {
        if (currentContext.currentLevel == null || currentContext.currentLevel.any { it !is PsiDDItem }) {
            logger.warn { "Some item from current level are not PsiWithBodyDDItem. The wrong lens is used. " }
            return
        }
        val currentLevel = currentContext.currentLevel as List<PsiDDItem>
        logger.info { "Built a trie for the current context" }
        if (items.any { it !is PsiDDItem }) {
            logger.warn { "Some items from $items are not PsiDDItem. The wrong lens is used. " }
            return
        }

        val items = items as List<PsiDDItem>
        logFocusedItems(items, currentContext)
        val currentLevelTrie = PsiItemStorage.create(
            currentLevel,
            currentLevel.toSet() - items.toSet(),
            currentContext,
        )

        currentLevelTrie.usedPaths.forEach { focusOnInsideFile(currentContext, currentLevelTrie, it) }
        logger.info { "Focusing complete" }
    }

    private suspend fun logFocusedItems(items: List<PsiDDItem>, context: IJDDContext) {
        if (!logger.isTraceEnabled) {
            return
        }
        val psiManager = service<MinimizationPsiManagerService>()
        val psiElements = items.map { PsiUtils.getPsiElementFromItem(context, it) }
        readAction {
            logger.trace {
                "Focusing on items: \n" +
                    psiElements.joinToString("\n") { "\t- ${it?.text}" }
            }
        }
    }

    abstract fun focusOnPsiElement(psiElement: PsiElement, context: IJDDContext)
    abstract fun getWriteCommandActionName(psiFile: KtFile, context: IJDDContext): String

    private suspend fun focusOnInsideFile(currentContext: IJDDContext, trie: PsiItemStorage, relativePath: Path) {
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
            trie.processMarkedElements(psiFile) { focusOnPsiElement(it, currentContext) }
        }
    }
}
