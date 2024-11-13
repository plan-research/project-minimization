package org.plan.research.minimization.plugin.psi

import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.PsiChildrenPathDDItem

import com.intellij.psi.PsiElement
import mu.KotlinLogging
import org.jetbrains.kotlin.psi.*

/**
 * A class that provides functionality to replace with `TODO()`
 * statement bodies of various Kotlin PSI elements within a given project context.
 *
 * @property context The context for the minimization process, containing the current project and related properties.
 */
class PsiBodyReplacer(private val context: IJDDContext) : PsiChildrenPathDDItem.PsiWithBodyTransformer<Unit> {
    private val logger = KotlinLogging.logger {}
    private val psiFactory = KtPsiFactory(context.indexProject)
    private lateinit var item: PsiChildrenPathDDItem

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
                logger.debug { "Replacing function body block: ${function.name} in ${function.containingFile.virtualFile.path}" }
                function.bodyBlockExpression?.replace(
                    psiFactory.createBlock(replacementText),
                )
            }

            hasBody -> {
                logger.debug { "Replacing function body without block: ${function.name} in ${function.containingFile.virtualFile.path}" }
                function.bodyExpression!!.replace(
                    psiFactory.createExpression(replacementText),
                )
            }

            else -> {}
        }
    }

    override fun transform(lambdaExpression: KtLambdaExpression) {
        val replacementText = getReplacementText(item, hasExplicitReturnType = false)

        logger.debug { "Replacing lambda expression in ${lambdaExpression.containingFile.virtualFile.path}" }
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

        logger.debug { "Replacing accessor body: ${accessor.name} in ${accessor.containingFile.virtualFile.path}" }
        when {
            accessor.bodyBlockExpression != null -> accessor.bodyBlockExpression!!.replace(
                psiFactory.createBlock(replacementText),
            )

            accessor.bodyExpression != null -> accessor.bodyExpression!!.replace(
                psiFactory.createExpression(replacementText),
            )
        }
    }

    fun transform(item: PsiChildrenPathDDItem, element: PsiElement) {
        this.item = item
        try {
            transform(element)
        } catch (e: Exception) {
            logger.error(e) { "Failed to transform element: ${element.text}" }
        }
    }

    private fun getReplacementText(
        item: PsiChildrenPathDDItem,
        hasExplicitReturnType: Boolean,
    ): String =
        when {
            hasExplicitReturnType || item.renderedType == null ->
                BLOCKLESS_TEXT

            else -> {
                val returnTypeText = item.renderedType
                "$BLOCKLESS_TEXT as $returnTypeText"
            }
        }

    companion object {
        private const val BLOCKLESS_TEXT = "TODO(\"Removed by DD\")"
    }
}
