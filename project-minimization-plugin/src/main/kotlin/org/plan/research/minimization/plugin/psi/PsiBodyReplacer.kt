package org.plan.research.minimization.plugin.psi

import org.plan.research.minimization.plugin.model.IJDDContext

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.command.writeCommandAction
import com.intellij.psi.PsiElement
import mu.KotlinLogging
import org.jetbrains.kotlin.psi.*

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A class that provides functionality to replace with `TODO()`
 * statement bodies of various Kotlin PSI elements within a given project context.
 *
 * @property context The context for the minimization process, containing the current project and related properties.
 */
class PsiBodyReplacer(private val context: IJDDContext) {
    private val logger = KotlinLogging.logger {}
    private val psiFactory = KtPsiFactory(context.indexProject)

    suspend fun replaceBody(classInitializer: KtClassInitializer) {
        withContext(Dispatchers.EDT) {
            writeCommandAction(context.indexProject, "Replacing Class Initializer") {
                logger.debug { "Replacing class initializer body: ${classInitializer.name}" }
                classInitializer.body?.replace(psiFactory.createBlock(BLOCKLESS_TEXT))
            }
        }
    }

    suspend fun replaceBody(function: KtNamedFunction) {
        val (hasBlockBody, hasBody) = readAction { function.hasBlockBody() to function.hasBody() }
        when {
            hasBlockBody -> withContext(Dispatchers.EDT) {
                writeCommandAction(context.indexProject, "Replacing Function Body Block") {
                    logger.debug { "Replacing function body block: ${function.name} in ${function.containingFile.virtualFile.path}" }
                    function.bodyBlockExpression?.replace(
                        psiFactory.createBlock(
                            BLOCKLESS_TEXT,
                        ),
                    )
                }
            }

            hasBody -> withContext(Dispatchers.EDT) {
                writeCommandAction(context.indexProject, "Replacing Function Body Expression") {
                    logger.debug { "Replacing function body without block: ${function.name} in ${function.containingFile.virtualFile.path}" }
                    function.bodyExpression!!.replace(
                        psiFactory.createExpression(
                            BLOCKLESS_TEXT,
                        ),
                    )
                }
            }

            else -> {}
        }
    }

    suspend fun replaceBody(lambdaExpression: KtLambdaExpression): Unit = withContext(Dispatchers.EDT) {
        writeCommandAction(context.indexProject, "Replacing Lambda Body Expression") {
            logger.debug { "Replacing lambda expression in ${lambdaExpression.containingFile.virtualFile.path}" }
            lambdaExpression.bodyExpression!!.replace(
                psiFactory.createLambdaExpression(
                    "",
                    BLOCKLESS_TEXT,
                ).bodyExpression!!,
            )
        }
    }

    suspend fun replaceBody(accessor: KtPropertyAccessor): Unit = withContext(Dispatchers.EDT) {
        writeCommandAction(context.indexProject, "Replacing Accessor Body") {
            logger.debug { "Replacing accessor body: ${accessor.name} in ${accessor.containingFile.virtualFile.path}" }
            when {
                accessor.bodyBlockExpression != null -> accessor.bodyBlockExpression!!.replace(psiFactory.createBlock(BLOCKLESS_TEXT))
                accessor.bodyExpression != null -> accessor.bodyExpression!!.replace(psiFactory.createExpression(BLOCKLESS_TEXT))
            }
        }
    }

    suspend fun replaceBody(element: PsiElement): Unit = when (element) {
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
