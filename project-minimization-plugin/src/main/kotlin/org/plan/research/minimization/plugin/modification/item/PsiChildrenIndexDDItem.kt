package org.plan.research.minimization.plugin.modification.item

import org.plan.research.minimization.plugin.modification.item.index.IntChildrenIndex

import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.PsiMethodImpl
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.idea.KotlinLanguage
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
        val JAVA_BODY_REPLACEABLE_PSI_JAVA_CLASSES = listOf(
            PsiMethodImpl::class.java,
        )
        val BODY_REPLACEABLE_PSI_JAVA_CLASSES: List<ClassKtExpression> =
            DECLARATIONS_WITH_BODY_JAVA_CLASSES + EXPRESSIONS_WITH_BODY_JAVA_CLASSES

        fun isCompatible(psiElement: PsiElement): Boolean {
            val isCompatibleClass = when (psiElement.language) {
                is KotlinLanguage -> BODY_REPLACEABLE_PSI_JAVA_CLASSES
                is JavaLanguage -> JAVA_BODY_REPLACEABLE_PSI_JAVA_CLASSES
                else -> return false
            }.any { it.isInstance(psiElement) }

            return isCompatibleClass && hasBodyIfAvailable(psiElement) != false
        }

        fun hasBodyIfAvailable(psiElement: PsiElement): Boolean? = when (psiElement.language) {
            is KotlinLanguage -> DECLARATIONS_WITH_BODY_JAVA_CLASSES
                .find { it.isInstance(psiElement) }
                ?.let { (psiElement as KtDeclarationWithBody).hasBody() }

            is JavaLanguage -> JAVA_BODY_REPLACEABLE_PSI_JAVA_CLASSES
                .find { it.isInstance(psiElement) }
                ?.let { (psiElement as PsiMethodImpl).body != null }

            else -> null
        }

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
                // TODO: Update error
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
        fun transform(method: PsiMethodImpl): T

        fun transform(element: PsiElement): T = when (element) {
            is KtClassInitializer -> transform(element)
            is KtNamedFunction -> transform(element)
            is KtLambdaExpression -> transform(element)
            is KtPropertyAccessor -> transform(element)
            is PsiMethodImpl -> transform(element)
            else -> error("Invalid PSI element type: ${element::class.simpleName}. Expected one of: KtClassInitializer, KtNamedFunction, KtLambdaExpression, KtPropertyAccessor")
        }
    }
}
