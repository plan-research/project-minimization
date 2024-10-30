package org.plan.research.minimization.plugin.psi

import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.psi.KtFile

import kotlin.io.path.relativeTo

class PsiProcessor(private val project: Project) {
    private val rootPath = project.guessProjectDir()!!.toNioPath()

    @RequiresReadLock
    fun getPsiElementParentPath(
        element: PsiElement,
    ): PsiWithBodyDDItem? {
        var currentElement: PsiElement? = element
        val path = buildList {
            while (currentElement?.parent != null && currentElement !is PsiFile) {
                val current = currentElement!!
                val parent = current.parent
                if (PsiWithBodyDDItem.isCompatible(parent)) {
                    return null
                }
                val position = getChildPosition(parent, current)
                add(position)
                currentElement = parent
            }
        }
        val lastElement = currentElement  // Some problems with Kotlin Compiler, smart casts error
        require(lastElement is PsiFile)
        val localPath = lastElement.virtualFile.toNioPath().relativeTo(rootPath)
        val parentPath = path.reversed()
        return PsiWithBodyDDItem.create(element, parentPath, localPath)
    }

    @RequiresReadLock
    private fun getChildPosition(parent: PsiElement, element: PsiElement): Int =
        parent.children.indexOf(element)

    @RequiresReadLock
    fun getKtFile(file: VirtualFile): KtFile? =
        PsiManagerEx.getInstance(project).findFile(file) as? KtFile
}
