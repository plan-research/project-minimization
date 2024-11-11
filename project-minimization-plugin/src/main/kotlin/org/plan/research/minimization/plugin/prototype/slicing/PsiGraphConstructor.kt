package org.plan.research.minimization.plugin.prototype.slicing

import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.psi.PsiUtils

@Service(Service.Level.PROJECT)
class PsiGraphConstructor(private val project: Project) {
    suspend fun getEdgesFromNode(node: PsiSlicingNode): List<PsiSlicingNode> {
        require(node.context.indexProject == project) { "Possible PSI Leak. Please consider to run this service from index project" }
        val psiElement =
            readAction { PsiUtils.getPsiElementOrFileFromItem(node.context, node.underlyingItem) } ?: return emptyList()
        return buildList {
            readAction {
                addAll(psiElement.children)
                addAll(KtPsiLookupUtils.lookupEverything(psiElement))
                psiElement.containingFile?.takeIf { psiElement !is PsiFile }?.let(this::add)
            }
        }
            .map { readAction { PsiSlicingNode(PsiUtils.buildPsiItem(node.context, it), node.context) } }
    }

    @RequiresReadLock
    fun initializeGraphNode(context: IJDDContext, file: PsiFile): PsiSlicingNode? {
        val item = PsiUtils.buildPsiItem(context, file)
        return PsiSlicingNode(item, context)
    }
}