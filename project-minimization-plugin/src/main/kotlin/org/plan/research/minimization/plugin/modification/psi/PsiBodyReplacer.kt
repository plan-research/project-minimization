package org.plan.research.minimization.plugin.modification.psi

import org.plan.research.minimization.plugin.context.IJDDContext
import org.plan.research.minimization.plugin.modification.item.PsiChildrenIndexDDItem

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.PsiMethodImpl
import com.intellij.psi.impl.source.tree.java.PsiLambdaExpressionImpl
import mu.KotlinLogging
import org.jetbrains.kotlin.idea.base.psi.getLineNumber
import org.jetbrains.kotlin.psi.*

/**
 * A class that provides functionality to replace with `TODO()`
 * statement bodies of various Kotlin PSI elements within a given project context.
 *
 * @property context The context for the minimization process, containing the current project and related properties.
 */
class PsiBodyReplacer(private val context: IJDDContext) : PsiChildrenIndexDDItem.PsiWithBodyTransformer<Unit> {
    private val logger = KotlinLogging.logger {}
    private val psiFactory = KtPsiFactory(context.indexProject)
    private val javaPsiFactory = JavaPsiFacade.getElementFactory(context.indexProject)
    private lateinit var item: PsiChildrenIndexDDItem

    override fun transform(classInitializer: KtClassInitializer) {
        logger.debug { "Replacing class initializer body: ${classInitializer.name}" }
        classInitializer.body?.replace(psiFactory.createBlock(BLOCKLESS_TEXT))
    }

    override fun transform(function: KtNamedFunction) {
        val explicitType = function.typeReference != null
        val replacementText = getReplacementText(item, explicitType)

        val (hasBlockBody, hasBody) = function.hasBlockBody() to function.hasBody()
        when {
            hasBlockBody -> {
                logger.trace { "Replacing function body block: ${function.name} in ${function.containingFile.virtualFile.path}" }
                function.bodyBlockExpression?.replace(
                    psiFactory.createBlock(replacementText),
                )
            }

            hasBody -> {
                logger.trace { "Replacing function body without block: ${function.name} in ${function.containingFile.virtualFile.path}" }
                function.bodyExpression!!.replace(
                    psiFactory.createExpression(replacementText),
                )
            }

            else -> {}
        }
    }

    override fun transform(lambdaExpression: KtLambdaExpression) {
        val replacementText = getReplacementText(item, hasExplicitReturnType = false)

        logger.trace { "Replacing lambda expression in ${lambdaExpression.containingFile.virtualFile.path}" }
        lambdaExpression.bodyExpression!!.replace(
            psiFactory.createLambdaExpression(
                "",
                replacementText,
            ).bodyExpression!!,
        )
    }

    override fun transform(accessor: KtPropertyAccessor) {
        val explicitType = accessor.returnTypeReference != null
        val replacementText = getReplacementText(item, explicitType)

        logger.trace { "Replacing accessor body: ${accessor.name} in ${accessor.containingFile.virtualFile.path}" }
        when {
            accessor.bodyBlockExpression != null -> accessor.bodyBlockExpression!!.replace(
                psiFactory.createBlock(replacementText),
            )

            accessor.bodyExpression != null -> accessor.bodyExpression!!.replace(
                psiFactory.createExpression(replacementText),
            )
        }
    }

    override fun transform(method: PsiMethodImpl) {
        logger.trace { "Replacing method body: ${method.name} in ${method.containingFile.virtualFile.path}" }
        method.body?.let {
            val block = javaPsiFactory.createCodeBlockFromText(JAVA_BLOCK_TEXT, it.context)
            try {
                it.replace(block)
            } catch (e: UnsupportedOperationException) {
                logger.error(e) { "Failed to replace body of method ${method.name}" }
            }
        } ?: run {
            logger.warn { "Method ${method.name} in ${method.containingFile.virtualFile.path} has no body" }
        }
    }

    override fun transform(lambdaExpression: PsiLambdaExpressionImpl) {
        logger.trace { "Replacing lambda at line ${lambdaExpression.getLineNumber()} in ${lambdaExpression.containingFile.virtualFile.path}" }
        lambdaExpression.body?.let {
            // NOTE: Lambda can have an expression as a body, but `throw` is not an expression,
            //       so code block is used as the new body in any case.
            val block = javaPsiFactory.createCodeBlockFromText(JAVA_BLOCK_TEXT, it.context)
            try {
                it.replace(block)
            } catch (e: UnsupportedOperationException) {
                logger.error(e) { "Failed to replace body of lambda expression at line ${lambdaExpression.getLineNumber()} in ${lambdaExpression.containingFile.virtualFile.path}" }
            }
        } ?: run {
            logger.warn { "Lambda expression at line ${lambdaExpression.getLineNumber()} in ${lambdaExpression.containingFile.virtualFile.path} has no body" }
        }
    }

    fun transform(item: PsiChildrenIndexDDItem, element: PsiElement) {
        this.item = item
        try {
            transform(element)
        } catch (e: Exception) {
            logger.error(e) { "Failed to transform element: ${element.text}" }
        }
    }

    private fun getReplacementText(
        item: PsiChildrenIndexDDItem,
        hasExplicitReturnType: Boolean,
    ): String = when {
        hasExplicitReturnType || item.renderedType == null ->
            BLOCKLESS_TEXT

        else -> {
            val returnTypeText = item.renderedType
            "$BLOCKLESS_TEXT as $returnTypeText"
        }
    }

    companion object {
        private const val BLOCKLESS_TEXT = "TODO(\"Removed by DD\")"
        private const val JAVA_BLOCK_TEXT = "{\nthrow new UnsupportedOperationException(\"Removed by DD\");\n}"
    }
}
