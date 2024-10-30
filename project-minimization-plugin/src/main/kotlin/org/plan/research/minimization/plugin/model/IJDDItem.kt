package org.plan.research.minimization.plugin.model

import org.plan.research.minimization.core.model.DDItem

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPropertyAccessor

import java.nio.file.Path

import kotlin.io.path.relativeTo

sealed interface IJDDItem : DDItem

/**
 * Represents a project file item in the minimization process.
 *
 * @property localPath The local path of the file within the project directory.
 * @constructor Creates an instance of [ProjectFileDDItem] with the specified local path.
 */
data class ProjectFileDDItem(val localPath: Path) : IJDDItem {
    fun getVirtualFile(context: IJDDContext): VirtualFile? =
        context.projectDir.findFileByRelativePath(localPath.toString())

    companion object {
        fun create(context: IJDDContext, virtualFile: VirtualFile): ProjectFileDDItem =
            ProjectFileDDItem(virtualFile.toNioPath().relativeTo(context.projectDir.toNioPath()))
    }
}

data class PsiWithBodyDDItem(
    val localPath: Path,
    val childrenPath: List<Int>,
) : IJDDItem {
    companion object {
        fun isCompatible(psiElement: PsiElement) = (psiElement is KtNamedFunction ||
            psiElement is KtClassInitializer ||
            psiElement is KtLambdaExpression ||
            psiElement is KtPropertyAccessor) && (psiElement !is KtDeclarationWithBody || psiElement.hasBody())

        @RequiresReadLock
        fun create(element: PsiElement, parentPath: List<Int>, localPath: Path): PsiWithBodyDDItem =
            if (isCompatible(element)) {
                PsiWithBodyDDItem(
                    localPath,
                    parentPath,
                )
            } else {
                error(
                    "Invalid Psi Element. " +
                        "Supported types: " +
                        "KtNamedFunction, KtClassInitializer, KtPropertyAccessor, KtLambdaExpression, " +
                        "but got ${element.javaClass.simpleName}",
                )
            }
    }
}
