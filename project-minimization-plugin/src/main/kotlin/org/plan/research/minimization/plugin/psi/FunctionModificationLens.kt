package org.plan.research.minimization.plugin.psi

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.Key
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtFile
import org.plan.research.minimization.plugin.acceptOnAllKotlinFiles
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.IJDDItem
import org.plan.research.minimization.plugin.model.ProjectItemLens
import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem
import org.plan.research.minimization.plugin.psi.ModifyingBodyKtVisitor.Companion.MAPPED_FOR_DELETION_KEY

class FunctionModificationLens : ProjectItemLens {
    override suspend fun focusOn(
        items: List<IJDDItem>,
        currentContext: IJDDContext
    ) {
        if (items.any { it !is PsiWithBodyDDItem }) return
        val items = items as List<PsiWithBodyDDItem>
        writeAction {
            items.forEach { item -> item.underlyingObject.element?.putUserData(MAPPED_FOR_DELETION_KEY, true) }
        }
        val visitor = ModifyingBodyKtVisitor(currentContext.originalProject, currentContext.project)
        readAction {
            currentContext.project.acceptOnAllKotlinFiles(visitor)
        }

        withContext(Dispatchers.EDT) { // For synchronization
            writeAction {
                items.forEach { item -> item.underlyingObject.element?.putUserData(MAPPED_FOR_DELETION_KEY, false) }
            }
        }
    }
}