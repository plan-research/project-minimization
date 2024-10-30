package org.plan.research.minimization.plugin.psi

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.command.writeCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import mu.KotlinLogging
import org.jetbrains.kotlin.psi.*

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service that provides functions to modify the bodies of various Kotlin elements within a project.
 *
 */
@Service(Service.Level.PROJECT)
class PsiModificationManager(private val rootProject: Project) {
    private val logger = KotlinLogging.logger {}
    private val psiFactory = KtPsiFactory(rootProject)

    suspend fun replaceBody(classInitializer: KtClassInitializer) {
        withContext(Dispatchers.EDT) {
            writeCommandAction(rootProject, "Replacing Class Initializer") {
                logger.debug { "Replacing class initializer body: ${classInitializer.name}" }
                classInitializer.body?.replace(psiFactory.createBlock(BLOCKLESS_TEXT))
            }
        }
    }

    suspend fun replaceBody(function: KtNamedFunction) {
        val (hasBlockBody, hasBody) = readAction { function.hasBlockBody() to function.hasBody() }
        when {
            hasBlockBody -> withContext(Dispatchers.EDT) {
                writeCommandAction(rootProject, "Replacing Function Body Block") {
                    logger.debug { "Replacing function body block: ${function.name} in ${function.containingFile.virtualFile.path}" }
                    function.bodyBlockExpression?.replace(
                        psiFactory.createBlock(
                            BLOCKLESS_TEXT,
                        ),
                    )
                }
            }

            hasBody -> withContext(Dispatchers.EDT) {
                writeCommandAction(rootProject, "Replacing Function Body Expression") {
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

    suspend fun replaceBody(lambdaExpression: KtLambdaExpression) = withContext(Dispatchers.EDT) {
        writeCommandAction(rootProject, "Replacing Lambda Body Expression") {
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
        writeCommandAction(rootProject, "Replacing Accessor Body") {
            logger.debug { "Replacing accessor body: ${accessor.name} in ${accessor.containingFile.virtualFile.path}" }
            when {
                accessor.hasBlockBody() -> accessor.bodyBlockExpression!!.replace(psiFactory.createBlock(BLOCKLESS_TEXT))
                accessor.hasBody() -> accessor.bodyExpression!!.replace(psiFactory.createExpression(BLOCKLESS_TEXT))
            }
        }
    }

    suspend fun replaceBody(element: PsiElement) = when (element) {
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
