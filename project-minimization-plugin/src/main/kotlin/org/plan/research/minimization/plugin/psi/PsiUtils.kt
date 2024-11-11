package org.plan.research.minimization.plugin.psi

import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem

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
    fun getPsiElementFromItem(context: IJDDContext, item: PsiWithBodyDDItem): KtExpression? {
        return getPsiElementOrFileFromItem(context, item) as? KtExpression
    }
    @RequiresReadLock
    fun getPsiElementOrFileFromItem(context: IJDDContext, item: PsiWithBodyDDItem): PsiElement? {
        val file = context.projectDir.findFileByRelativePath(item.localPath.toString()) ?: return null
        val ktFile = getKtFile(context, file) ?: return null
        var currentDepth = 0
        var element: PsiElement = ktFile
        while (currentDepth < item.childrenPath.size) {
            element = element.children[item.childrenPath[currentDepth++]]
        }
        return element
    }

    // DELETE THIS AND MERGE khbminus/unused-object-deletion INTO HERE
    @RequiresReadLock
    fun buildPsiItem(context: IJDDContext, element: PsiElement): PsiWithBodyDDItem {
        val (file, path) = buildParentPath(element)!!
        return PsiWithBodyDDItem(file.virtualFile.toNioPath().relativeTo(context.projectDir.toNioPath()), path)
    }

    // DELETE THIS AND MERGE khbminus/unused-object-deletion INTO HERE
    @RequiresReadLock
    private fun buildParentPath(
        element: PsiElement,
    ): Pair<PsiFile, List<Int>>? {
        var currentElement: PsiElement = element
        val path = buildList {
            while (currentElement.parent != null && currentElement !is PsiFile) {
                val parent = currentElement.parent
                val position = getChildPosition(parent, currentElement)
                add(position)
                currentElement = parent
            }
        }
        require(currentElement is PsiFile)
        return (currentElement as PsiFile) to path.reversed()
    }

    @RequiresReadLock
    fun createPsiDDItem(
        context: IJDDContext,
        element: PsiElement,
    ): PsiWithBodyDDItem? {
        var currentElement: PsiElement = element
        val path = buildList {
            while (currentElement.parent != null && currentElement !is PsiFile) {
                val parent = currentElement.parent
                if (PsiWithBodyDDItem.isCompatible(parent)) {
                    return null
                }
                val position = getChildPosition(parent, currentElement)
                add(position)
                currentElement = parent
            }
        }
        require(currentElement is PsiFile)
        val currentFile = currentElement as PsiFile
        val localPath = currentFile.virtualFile.toNioPath().relativeTo(context.projectDir.toNioPath())
        val parentPath = path.reversed()
        return PsiWithBodyDDItem.create(element, parentPath, localPath)
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
    ): T? = withContext(Dispatchers.IO) {
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
