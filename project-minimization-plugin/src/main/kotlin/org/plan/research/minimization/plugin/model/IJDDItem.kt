package org.plan.research.minimization.plugin.model

import org.plan.research.minimization.core.model.DDItem

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor

import java.nio.file.Path

import kotlin.io.path.relativeTo

typealias ClassKtExpression = Class<out KtExpression>
private typealias ClassDeclarationWithBody = Class<out KtDeclarationWithBody>

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

data class PsiDDItem(
    val localPath: Path,
    val childrenPath: List<Int>,
) : IJDDItem {
    companion object {
        val DECLARATIONS_WITH_BODY_JAVA_CLASSES: List<ClassDeclarationWithBody> = listOf(
            KtNamedFunction::class.java,
            KtPropertyAccessor::class.java,
        )
        val EXPRESSIONS_WITH_BODY_JAVA_CLASSES: List<ClassKtExpression> = listOf(
            KtLambdaExpression::class.java,
            KtClassInitializer::class.java,
        )
        val DELETABLE_PSI_JAVA_CLASSES: List<ClassKtExpression> = listOf(
            KtNamedFunction::class.java,
            KtClass::class.java,
            KtObjectDeclaration::class.java,
            KtProperty::class.java,
        )
        val BODY_REPLACEABLE_PSI_JAVA_CLASSES: List<ClassKtExpression> =
            DECLARATIONS_WITH_BODY_JAVA_CLASSES + EXPRESSIONS_WITH_BODY_JAVA_CLASSES

        fun isCompatible(psiElement: PsiElement) =
            BODY_REPLACEABLE_PSI_JAVA_CLASSES.any { it.isInstance(psiElement) } && hasBodyIfAvailable(psiElement) != false

        fun hasBodyIfAvailable(psiElement: PsiElement): Boolean? = DECLARATIONS_WITH_BODY_JAVA_CLASSES
            .find { it.isInstance(psiElement) }
            ?.let { (psiElement as KtDeclarationWithBody) }
            ?.hasBody()

        fun create(parentPath: List<Int>, localPath: Path): PsiDDItem = PsiDDItem(
            localPath,
            parentPath,
        )
    }
}
