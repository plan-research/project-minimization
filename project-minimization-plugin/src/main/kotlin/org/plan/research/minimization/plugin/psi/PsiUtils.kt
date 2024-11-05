package org.plan.research.minimization.plugin.psi

import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile

import kotlin.io.path.relativeTo

/**
 * The PsiProcessor class provides utilities for fetching PSI elements within a given project.
 */
object PsiUtils {
    @RequiresReadLock
    fun getPsiElementFromItem(context: IJDDContext, item: PsiWithBodyDDItem): KtExpression? {
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
}