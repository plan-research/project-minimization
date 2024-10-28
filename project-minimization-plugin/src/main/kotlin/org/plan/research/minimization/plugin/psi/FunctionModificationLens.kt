package org.plan.research.minimization.plugin.psi

import org.plan.research.minimization.plugin.acceptOnAllKotlinFiles
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.IJDDItem
import org.plan.research.minimization.plugin.model.ProjectItemLens
import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem
import org.plan.research.minimization.plugin.psi.ModifyingBodyKtVisitor.Companion.MAPPED_AS_STORED_KEY

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import mu.KotlinLogging

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        if (items.any { it !is PsiWithBodyDDItem }) {
            logger.warn { "Some items from $items are not PsiWithBodyDDItem. The wrong lens is used. " }
            return
        }
        val items = items as List<PsiWithBodyDDItem>
        writeAction {
            logger.debug {
                "Focusing on:\n" + items.joinToString("\n") {
                    "\t- ${it.underlyingObject.element?.text?.lines()?.joinToString("\n") { "\t  $it" }}"
                }
            }
        }
        logger.info { "Storing stored information into underlying PSI element" }
        writeAction {
            items.forEach { item -> item.underlyingObject.element?.putUserData(MAPPED_AS_STORED_KEY, true) }
        }
        val visitor = readAction { ModifyingBodyKtVisitor(currentContext.originalProject, currentContext.project) }
        readAction {
            currentContext.project.acceptOnAllKotlinFiles(visitor)
        }
        logger.info { "PSI Element Visitor has finished successfully" }

        withContext(Dispatchers.EDT) {
            // For synchronization
            writeAction {
                items.forEach { item -> item.underlyingObject.element?.putUserData(MAPPED_AS_STORED_KEY, false) }
            }
        }
        logger.info { "All PSI keys returned to the initial state. The focus is complete" }
    }
}
