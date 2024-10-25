package org.plan.research.minimization.plugin.model

import org.plan.research.minimization.core.model.DDItem

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.psi.KtClassInitializer
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

sealed interface PsiWithBodyDDItem : IJDDItem {
    val underlyingObject: SmartPsiElementPointer<out PsiElement>

    data class ClassInitializer(override val underlyingObject: SmartPsiElementPointer<KtClassInitializer>) :
        PsiWithBodyDDItem

    data class LambdaExpression(override val underlyingObject: SmartPsiElementPointer<KtLambdaExpression>) :
        PsiWithBodyDDItem

    data class PropertyAccessor(override val underlyingObject: SmartPsiElementPointer<KtPropertyAccessor>) :
        PsiWithBodyDDItem

    data class NamedFunctionWithBlock(override val underlyingObject: SmartPsiElementPointer<KtNamedFunction>) :
        PsiWithBodyDDItem

    data class NamedFunctionWithoutBlock(override val underlyingObject: SmartPsiElementPointer<KtNamedFunction>) :
        PsiWithBodyDDItem
}
