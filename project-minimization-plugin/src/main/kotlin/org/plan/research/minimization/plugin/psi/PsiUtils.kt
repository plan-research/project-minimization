package org.plan.research.minimization.plugin.psi

import org.plan.research.minimization.plugin.model.IJDDContext
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

/**
 * The PsiProcessor class provides utilities for fetching PSI elements within a given project.
 */
object PsiUtils {
    @RequiresReadLock
    fun getPsiElementFromItem(context: IJDDContext, item: PsiDDItem): KtExpression? {
        val file = context.projectDir.findFileByRelativePath(item.localPath.toString())!!
        val ktFile = getKtFile(context, file)!!
        var currentDepth = 0
        var element: PsiElement = ktFile
        while (currentDepth < item.childrenPath.size) {
            element = element.children[item.childrenPath[currentDepth++]]
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
    ): PsiDDItem? {
        val (currentFile, parentPath) = buildParentPath(element) { !PsiDDItem.isCompatible(it) } ?: return null
        val localPath = currentFile.virtualFile.toNioPath().relativeTo(context.projectDir.toNioPath())
        return PsiDDItem.create(parentPath, localPath)
    }

    /**
     * Transforms PsiElement into **deletable** PsiDDItem by traversing the parents and collecting file information
     *
     * @param element Element to transform
     * @param context
     * @return a converted PSIDDItem
     */
    @RequiresReadLock
    fun buildDeletablePsiItem(
        context: IJDDContext,
        element: PsiElement,
    ): PsiDDItem {
        val (currentFile, parentPath) = buildParentPath(element) { true }!!
        val localPath = currentFile.virtualFile.toNioPath().relativeTo(context.projectDir.toNioPath())
        return PsiDDItem.create(parentPath, localPath)
    }

    @RequiresReadLock
    private fun buildParentPath(
        element: PsiElement,
        isElementAllowed: (PsiElement) -> Boolean,
    ): Pair<PsiFile, List<Int>>? {
        var currentElement: PsiElement = element
        val path = buildList {
            while (currentElement.parent != null && currentElement !is PsiFile) {
                val parent = currentElement.parent
                if (!isElementAllowed(parent)) {
                    return null
                }
                val position = getChildPosition(parent, currentElement)
                add(position)
                currentElement = parent
            }
        }
        require(currentElement is PsiFile)
        return (currentElement as PsiFile) to path.reversed()
    }

    @RequiresReadLock
    private fun getChildPosition(parent: PsiElement, element: PsiElement): Int =
        parent.children.indexOf(element)

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
