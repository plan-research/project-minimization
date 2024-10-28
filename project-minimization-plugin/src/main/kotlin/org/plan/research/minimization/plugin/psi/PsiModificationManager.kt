package org.plan.research.minimization.plugin.psi

import com.intellij.openapi.application.EDT
import com.intellij.openapi.command.writeCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import mu.KotlinLogging
import org.jetbrains.kotlin.psi.*

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Service that provides functions to modify the bodies of various Kotlin elements within a project.
 *
 */
@Service(Service.Level.PROJECT)
class PsiModificationManager(private val rootProject: Project, private val cs: CoroutineScope) {
    private val logger = KotlinLogging.logger {}
    private val psiFactory = KtPsiFactory(rootProject)

    fun replaceBody(classInitializer: KtClassInitializer) {
        cs.launch(Dispatchers.EDT) {
            writeCommandAction(rootProject, "Replacing Class Initializer") {
                logger.debug { "Replacing class initializer body: ${classInitializer.name}" }
                classInitializer.body?.replace(psiFactory.createBlock(BLOCKLESS_TEXT))
            }
        }
    }

    fun replaceBody(function: KtNamedFunction) = when {
        function.hasBlockBody() -> cs.launch(Dispatchers.EDT) {
            writeCommandAction(rootProject, "Replacing Function Body Block") {
                logger.debug { "Replacing function body block: ${function.name} in ${function.containingFile.virtualFile.path}" }
                function.bodyBlockExpression?.replace(
                    psiFactory.createBlock(
                        BLOCKLESS_TEXT,
                    ),
                )
            }
        }

        function.hasBody() -> cs.launch(Dispatchers.EDT) {
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

    fun replaceBody(lambdaExpression: KtLambdaExpression) = cs.launch(Dispatchers.EDT) {
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

    fun replaceBody(accessor: KtPropertyAccessor) = cs.launch(Dispatchers.EDT) {
        writeCommandAction(rootProject, "Replacing Accessor Body") {
            logger.debug { "Replacing accessor body: ${accessor.name} in ${accessor.containingFile.virtualFile.path}" }
            when {
                accessor.hasBlockBody() -> accessor.bodyBlockExpression!!.replace(psiFactory.createBlock(BLOCKLESS_TEXT))
                accessor.hasBody() -> accessor.bodyExpression!!.replace(psiFactory.createExpression(BLOCKLESS_TEXT))
            }
        }
    }

    companion object {
        private const val BLOCKLESS_TEXT = "TODO(\"Removed by DD\")"
    }
}
