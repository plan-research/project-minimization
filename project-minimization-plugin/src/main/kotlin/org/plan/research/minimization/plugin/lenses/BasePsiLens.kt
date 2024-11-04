package org.plan.research.minimization.plugin.lenses

import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.IJDDItem
import org.plan.research.minimization.plugin.model.ProjectItemLens
import org.plan.research.minimization.plugin.model.PsiDDItem
import org.plan.research.minimization.plugin.psi.PsiItemStorage
import org.plan.research.minimization.plugin.services.MinimizationPsiManager

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.findFile
import com.intellij.psi.PsiElement
import mu.KotlinLogging
import org.jetbrains.kotlin.idea.core.util.toPsiFile
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
            logger.warn { "Some items from $items are not PsiWithBodyDDItem. The wrong lens is used. " }
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
        val psiManager = context.project.service<MinimizationPsiManager>()
        val psiElements = items.map { psiManager.getPsiElementFromItem(it) }
        readAction {
            logger.trace {
                "Focusing on items: \n" +
                    psiElements.joinToString("\n") { "\t- ${it?.text}" }
            }
        }
    }

    abstract suspend fun focusOnPsiElement(psiElement: PsiElement, context: IJDDContext)

    private suspend fun focusOnInsideFile(currentContext: IJDDContext, trie: PsiItemStorage, relativePath: Path) {
        val virtualFile = smartReadAction(currentContext.project) {
            currentContext.projectDir.findFile(relativePath.toString())
        }
        virtualFile ?: run {
            logger.error { "The desired path for focused path $relativePath is not found in the project (name=${currentContext.project.name})" }
            return
        }
        val psiFile = readAction { virtualFile.toPsiFile(currentContext.project) as? KtFile }
        psiFile ?: run {
            logger.error { "The desired path for focused path $relativePath is not a Kotlin file in the project (name=${currentContext.project.name})" }
            return
        }
        logger.info { "Processing all focused elements in $relativePath" }
        trie.processMarkedElements(psiFile) { focusOnPsiElement(it, currentContext) }
    }
}
