package org.plan.research.minimization.plugin.lenses

import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.IJDDItem
import org.plan.research.minimization.plugin.model.ProjectItemLens
import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem
import org.plan.research.minimization.plugin.psi.PsiBodyReplacer
import org.plan.research.minimization.plugin.psi.PsiItemStorage
import org.plan.research.minimization.plugin.psi.PsiUtils
import org.plan.research.minimization.plugin.psi.PsiUtils.performPsiChangesAndSave

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.vfs.findFile
import mu.KotlinLogging

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
        val psiElements = items.map { readAction { PsiUtils.getPsiElementFromItem(context, it) } }
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
        val psiFile = smartReadAction(currentContext.indexProject) {
            PsiUtils.getKtFile(currentContext, virtualFile)
        }
        psiFile ?: run {
            logger.error { "The desired path for focused path $relativePath is not a Kotlin file in the project (name=${currentContext.projectDir.name})" }
            return
        }
        logger.debug { "Processing all focused elements in $relativePath" }
        val psiBodyReplacer = PsiBodyReplacer(currentContext)
        performPsiChangesAndSave(currentContext, psiFile, "Replace bodies inside ${psiFile.name}") {
            trie.processMarkedElements(psiFile, psiBodyReplacer::transform)
        }
    }
}
