package org.plan.research.minimization.plugin.psi

import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.IntWrapper
import org.plan.research.minimization.plugin.model.PsiChildrenPathDDItem
import org.plan.research.minimization.plugin.model.PsiChildrenPathIndex
import org.plan.research.minimization.plugin.model.PsiDDItem

import com.intellij.openapi.application.EDT
import com.intellij.openapi.command.writeCommandAction
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile

import kotlin.io.path.relativeTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private typealias ParentChildPsiProcessor<T> = (PsiElement, PsiElement) -> T

/**
 * The PsiProcessor class provides utilities for fetching PSI elements within a given project.
 */
object PsiUtils {
    @RequiresReadLock
    fun<T : PsiChildrenPathIndex> getPsiElementFromItem(context: IJDDContext, item: PsiDDItem<T>): KtExpression? {
        val file = context.projectDir.findFileByRelativePath(item.localPath.toString())!!
        val ktFile = getKtFile(context, file)!!
        var currentDepth = 0
        var element: PsiElement = ktFile
        while (currentDepth < item.childrenPath.size) {
            element = item.childrenPath[currentDepth++].getNext(element) ?: return null
        }
        val psiElement = element as? KtExpression
        return psiElement
    }

    /**
     * Transforms PsiElement into **replaceable** PSIDDItem by traversing the parents and collecting file information.
     * May return null if the element has a parent that could by modified
     *
     * @param element
     * @param context
     */
    @RequiresReadLock
    fun buildReplaceablePsiItem(
        context: IJDDContext,
        element: PsiElement,
    ): PsiChildrenPathDDItem? {
        val (currentFile, parentPath) = buildParentPath(
            element,
            ::getChildPosition,
        ) { !PsiChildrenPathDDItem.isCompatible(it) } ?: return null
        val localPath = currentFile.virtualFile.toNioPath().relativeTo(context.projectDir.toNioPath())
        val renderedType = PsiBodyTypeRenderer.transform(element)
        return PsiChildrenPathDDItem.create(element, parentPath, localPath, renderedType)
    }

    @RequiresReadLock
    private fun <T> buildParentPath(
        element: PsiElement,
        pathElementProducer: ParentChildPsiProcessor<T>,
        isElementAllowed: (PsiElement) -> Boolean,
    ): Pair<PsiFile, List<T>>? {
        var currentElement: PsiElement = element
        val path = buildList {
            while (currentElement.parent != null && currentElement !is PsiFile) {
                val parent = currentElement.parent
                if (!isElementAllowed(parent)) {
                    return null
                }
                val position = pathElementProducer(parent, currentElement)
                add(position)
                currentElement = parent
            }
        }
        require(currentElement is PsiFile)
        return (currentElement as PsiFile) to path.reversed()
    }

    @RequiresReadLock
    private fun getChildPosition(parent: PsiElement, element: PsiElement): IntWrapper =
        IntWrapper(parent.children.indexOf(element))

    @RequiresReadLock
    fun getKtFile(context: IJDDContext, file: VirtualFile): KtFile? =
        PsiManagerEx.getInstance(context.indexProject).findFile(file) as? KtFile

    suspend inline fun <T> performPsiChangesAndSave(
        context: IJDDContext,
        psiFile: PsiFile,
        commandName: String = "",
        crossinline block: () -> T,
    ): T? = withContext(Dispatchers.EDT) {
        writeCommandAction(context.indexProject, commandName) {
            val documentManager = PsiDocumentManager.getInstance(context.indexProject)
            val document = documentManager.getDocument(psiFile) ?: return@writeCommandAction null

            val editor = EditorFactory.getInstance().createEditor(document, context.indexProject)
            try {
                block()
            } finally {
                documentManager.doPostponedOperationsAndUnblockDocument(document)
                documentManager.commitDocument(document)
                FileDocumentManager.getInstance().saveDocument(document)

                EditorFactory.getInstance().releaseEditor(editor)
            }
        }
    }
}
