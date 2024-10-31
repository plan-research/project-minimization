package org.plan.research.minimization.plugin.psi

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.psi.KtFile
import org.plan.research.minimization.plugin.model.IJDDContext

import kotlin.io.path.relativeTo

/**
 * The PsiProcessor class provides utilities for fetching PSI elements within a given project.
 *
 */
class PsiProcessor(private val project: Project) {
    private val rootPath = project.guessProjectDir()!!.toNioPath()

    @RequiresReadLock
    fun getPsiElementParentPath(
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
        val localPath = currentFile.virtualFile.toNioPath().relativeTo(rootPath)
        val parentPath = path.reversed()
        return PsiWithBodyDDItem.create(element, parentPath, localPath)
    }

    @RequiresReadLock
    private fun getChildPosition(parent: PsiElement, element: PsiElement): Int =
        parent.children.indexOf(element)

    @RequiresReadLock
    fun getKtFile(file: VirtualFile): KtFile? =
        PsiManagerEx.getInstance(this@PsiProcessor.project).findFile(file) as? KtFile
}
