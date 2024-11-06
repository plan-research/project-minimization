package org.plan.research.minimization.plugin.psi

import com.intellij.psi.PsiElement
import mu.KotlinLogging
import org.jetbrains.kotlin.psi.*
import org.plan.research.minimization.plugin.model.IJDDContext

/**
 * A class that provides functionality to replace with `TODO()`
 * statement bodies of various Kotlin PSI elements within a given project context.
 *
 * @property context The context for the minimization process, containing the current project and related properties.
 */
class PsiBodyReplacer(private val context: IJDDContext) {
    private val logger = KotlinLogging.logger {}
    private val psiFactory = KtPsiFactory(context.indexProject)

    fun replaceBody(classInitializer: KtClassInitializer) {
        logger.debug { "Replacing class initializer body: ${classInitializer.name}" }
        classInitializer.body?.replace(psiFactory.createBlock(BLOCKLESS_TEXT))
    }

    fun replaceBody(function: KtNamedFunction) {
        val (hasBlockBody, hasBody) = function.hasBlockBody() to function.hasBody()
        when {
            hasBlockBody -> {
                logger.debug { "Replacing function body block: ${function.name} in ${function.containingFile.virtualFile.path}" }
                function.bodyBlockExpression?.replace(
                    psiFactory.createBlock(
                        BLOCKLESS_TEXT,
                    ),
                )
            }

            hasBody -> {
                logger.debug { "Replacing function body without block: ${function.name} in ${function.containingFile.virtualFile.path}" }
                function.bodyExpression!!.replace(
                    psiFactory.createExpression(
                        BLOCKLESS_TEXT,
                    ),
                )
            }

            else -> {}
        }
    }

    fun replaceBody(lambdaExpression: KtLambdaExpression) {
        logger.debug { "Replacing lambda expression in ${lambdaExpression.containingFile.virtualFile.path}" }
        lambdaExpression.bodyExpression!!.replace(
            psiFactory.createLambdaExpression(
                "",
                BLOCKLESS_TEXT,
            ).bodyExpression!!,
        )
    }

    fun replaceBody(accessor: KtPropertyAccessor) {
        logger.debug { "Replacing accessor body: ${accessor.name} in ${accessor.containingFile.virtualFile.path}" }
        when {
            accessor.bodyBlockExpression != null -> accessor.bodyBlockExpression!!.replace(
                psiFactory.createBlock(
                    BLOCKLESS_TEXT
                )
            )

            accessor.bodyExpression != null -> accessor.bodyExpression!!.replace(
                psiFactory.createExpression(
                    BLOCKLESS_TEXT
                )
            )
        }
    }

    fun replaceBody(element: PsiElement): Unit = when (element) {
        is KtClassInitializer -> replaceBody(element)
        is KtNamedFunction -> replaceBody(element)
        is KtLambdaExpression -> replaceBody(element)
        is KtPropertyAccessor -> replaceBody(element)
        else -> error("Invalid PSI element type: ${element::class.simpleName}. Expected one of: KtClassInitializer, KtNamedFunction, KtLambdaExpression, KtPropertyAccessor")
    }

    companion object {
        private const val BLOCKLESS_TEXT = "TODO(\"Removed by DD\")"
    }
}
