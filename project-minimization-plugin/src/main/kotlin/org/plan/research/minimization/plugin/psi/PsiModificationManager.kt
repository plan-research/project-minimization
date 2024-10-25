package org.plan.research.minimization.plugin.psi

import com.intellij.openapi.application.EDT
import com.intellij.openapi.command.writeCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.*

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class PsiModificationManager(private val rootProject: Project, private val cs: CoroutineScope) {
    private val psiFactory = KtPsiFactory(rootProject)

    fun replaceBody(classInitializer: KtClassInitializer) {
        cs.launch(Dispatchers.EDT) {
            writeCommandAction(rootProject, "Replacing Class Initializer") {
                classInitializer.body?.replace(psiFactory.createBlock(BLOCKLESS_TEXT))
            }
        }
    }

    fun replaceBody(function: KtNamedFunction) = when {
        function.hasBlockBody() -> cs.launch(Dispatchers.EDT) {
            writeCommandAction(rootProject, "Replacing Function Body Block") {
                function.bodyBlockExpression!!.replace(
                    psiFactory.createBlock(
                        BLOCKLESS_TEXT,
                    ),
                )
            }
        }

        function.hasBody() -> cs.launch(Dispatchers.EDT) {
            writeCommandAction(rootProject, "Replacing Function Body Expression") {
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
            lambdaExpression.bodyExpression!!.replace(psiFactory.createLambdaExpression("", BLOCKLESS_TEXT).bodyExpression!!)
        }
    }

    fun replaceBody(accessor: KtPropertyAccessor) = cs.launch(Dispatchers.EDT) {
        writeCommandAction(rootProject, "Replacing Accessor Body") {
            when {
                accessor.hasBlockBody() -> accessor.bodyBlockExpression!!.replace(psiFactory.createBlock(BLOCKLESS_TEXT))
                accessor.hasBody() -> accessor.bodyExpression!!.replace(psiFactory.createExpression(BLOCKLESS_TEXT))
            }
        }
    }

    companion object {
        private const val BLOCKLESS_TEXT = "TODO(\"Removed by DD\")"
        private const val BLOCK_TEXT = "{ $BLOCKLESS_TEXT) }"
    }
}
