package org.plan.research.minimization.plugin.lenses

import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.IJDDItem
import org.plan.research.minimization.plugin.model.ProjectItemLens
import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem
import org.plan.research.minimization.plugin.psi.PsiItemStorage
import org.plan.research.minimization.plugin.services.MinimizationPsiManager

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.findFile
import mu.KotlinLogging
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtFile

import java.nio.file.Path

/**
 * A lens that focuses on functions within a project.
 * It ensures that relevant function elements
 * are marked, processed, and reset appropriately within the given context.
 */
class FunctionModificationLens : ProjectItemLens {
    private val logger = KotlinLogging.logger {}
    override suspend fun focusOn(
        items: List<IJDDItem>,
        currentContext: IJDDContext,
    ) {
        if (currentContext.currentLevel == null || currentContext.currentLevel.any { it !is PsiWithBodyDDItem }) {
            logger.warn { "Some item from current level are not PsiWithBodyDDItem. The wrong lens is used. " }
            return
        }
        val currentLevel = currentContext.currentLevel as List<PsiWithBodyDDItem>
        logger.info { "Built a trie for the current context" }
        if (items.any { it !is PsiWithBodyDDItem }) {
            logger.warn { "Some items from $items are not PsiWithBodyDDItem. The wrong lens is used. " }
            return
        }

        val items = items as List<PsiWithBodyDDItem>
        logFocusedItems(items, currentContext)
        val currentLevelTrie = PsiItemStorage.create(
            currentLevel,
            currentLevel.toSet() - items.toSet(),
            currentContext,
        )

        currentLevelTrie.usedPaths.forEach { focusOnInsideFile(currentContext, currentLevelTrie, it) }
        logger.info { "Focusing complete" }
    }

    private suspend fun logFocusedItems(items: List<PsiWithBodyDDItem>, context: IJDDContext) {
        if (!logger.isTraceEnabled) {
            return
        }
        val psiManager = context.indexProject.service<MinimizationPsiManager>()
        val psiElements = items.map { psiManager.getPsiElementFromItem(it) }
        readAction {
            logger.trace {
                "Focusing on items: \n" +
                    psiElements.joinToString("\n") { "\t- ${it?.text}" }
            }
        }
    }

    private suspend fun focusOnInsideFile(currentContext: IJDDContext, trie: PsiItemStorage, relativePath: Path) {
        val virtualFile = readAction {
            currentContext.projectDir.findFile(relativePath.toString())
        }
        virtualFile ?: run {
            logger.error { "The desired path for focused path $relativePath is not found in the project (name=${currentContext.projectDir.name})" }
            return
        }
        val psiFile = readAction { virtualFile.toPsiFile(currentContext.indexProject) as? KtFile }
        psiFile ?: run {
            logger.error { "The desired path for focused path $relativePath is not a Kotlin file in the project (name=${currentContext.projectDir.name})" }
            return
        }
        val psiManager = currentContext.indexProject.service<MinimizationPsiManager>()
        logger.info { "Processing all focused elements in $relativePath" }
        trie.processMarkedElements(psiFile, psiManager::replaceBody)
    }
}
