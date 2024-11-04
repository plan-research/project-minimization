package org.plan.research.minimization.plugin.psi

import org.plan.research.minimization.plugin.model.PsiDDItem

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.psi.KtFile

import kotlin.io.path.relativeTo

/**
 * The PsiProcessor class provides utilities for fetching PSI elements within a given project.
 *
 */
class PsiProcessor(private val project: Project) {
    private val rootPath = project.guessProjectDir()!!.toNioPath()

    @RequiresReadLock
    fun buildReplaceablePsiItem(
        element: PsiElement,
    ): PsiDDItem? {
        val (currentFile, parentPath) = buildParentPath(element) { !PsiDDItem.isCompatible(it) } ?: return null
        val localPath = currentFile.virtualFile.toNioPath().relativeTo(rootPath)
        return PsiDDItem.create(parentPath, localPath)
    }

    fun buildDeletablePsiItem(
        element: PsiElement,
    ): PsiDDItem {
        val (currentFile, parentPath) = buildParentPath(element) { true }!!
        val localPath = currentFile.virtualFile.toNioPath().relativeTo(rootPath)
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
    fun getKtFile(file: VirtualFile): KtFile? =
        PsiManagerEx.getInstance(this@PsiProcessor.project).findFile(file) as? KtFile
}
