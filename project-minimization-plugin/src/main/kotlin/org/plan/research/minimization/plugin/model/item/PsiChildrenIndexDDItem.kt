package org.plan.research.minimization.plugin.model.item

import org.plan.research.minimization.plugin.model.item.index.IntChildrenIndex

import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.psi.*

import java.nio.file.Path

data class PsiChildrenIndexDDItem(
    override val localPath: Path,
    override val childrenPath: List<IntChildrenIndex>,
    val renderedType: String?,
) : PsiDDItem<IntChildrenIndex> {
    companion object {
        val DECLARATIONS_WITH_BODY_JAVA_CLASSES: List<ClassDeclarationWithBody> = listOf(
            KtNamedFunction::class.java,
            KtPropertyAccessor::class.java,
        )
        val EXPRESSIONS_WITH_BODY_JAVA_CLASSES: List<ClassKtExpression> = listOf(
            KtLambdaExpression::class.java,
            KtClassInitializer::class.java,
        )
        val BODY_REPLACEABLE_PSI_JAVA_CLASSES: List<ClassKtExpression> =
            DECLARATIONS_WITH_BODY_JAVA_CLASSES + EXPRESSIONS_WITH_BODY_JAVA_CLASSES

        fun isCompatible(psiElement: PsiElement) =
            BODY_REPLACEABLE_PSI_JAVA_CLASSES.any { it.isInstance(psiElement) } && hasBodyIfAvailable(psiElement) != false

        fun hasBodyIfAvailable(psiElement: PsiElement): Boolean? = DECLARATIONS_WITH_BODY_JAVA_CLASSES
            .find { it.isInstance(psiElement) }
            ?.let { (psiElement as KtDeclarationWithBody) }
            ?.hasBody()

        @RequiresReadLock
        fun create(
            element: PsiElement,
            parentPath: List<IntChildrenIndex>,
            localPath: Path,
            renderedType: String?,
        ): PsiChildrenIndexDDItem =
            if (isCompatible(element)) {
                PsiChildrenIndexDDItem(
                    localPath,
                    parentPath,
                    renderedType,
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

    interface PsiWithBodyTransformer<T> {
        fun transform(classInitializer: KtClassInitializer): T
        fun transform(function: KtNamedFunction): T
        fun transform(lambdaExpression: KtLambdaExpression): T
        fun transform(accessor: KtPropertyAccessor): T

        fun transform(element: PsiElement): T = when (element) {
            is KtClassInitializer -> transform(element)
            is KtNamedFunction -> transform(element)
            is KtLambdaExpression -> transform(element)
            is KtPropertyAccessor -> transform(element)
            else -> error("Invalid PSI element type: ${element::class.simpleName}. Expected one of: KtClassInitializer, KtNamedFunction, KtLambdaExpression, KtPropertyAccessor")
        }
    }
}
