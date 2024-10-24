package org.plan.research.minimization.plugin.psi

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.*

@Service(Service.Level.PROJECT)
class TopLevelFunctionModifierManager(private val rootProject: Project) {
    private val psiFactory = KtPsiFactory(rootProject)

    fun replaceBody(classInitializer: KtClassInitializer) {
        invokeLater { classInitializer.body?.replace(psiFactory.createBlock(BLOCK_TEXT)) }
    }

    fun replaceBody(function: KtNamedFunction) = when {
        function.hasBlockBody() -> invokeLater {
            function.bodyBlockExpression!!.replace(
                psiFactory.createBlock(
                    BLOCK_TEXT
                )
            )
        }

        function.hasBody() -> invokeLater {
            function.bodyExpression!!.replace(
                psiFactory.createExpression(
                    BLOCKLESS_TEXT
                )
            )
        }

        else -> {}
    }

    fun replaceBody(lambdaExpression: KtLambdaExpression) = invokeLater {
        lambdaExpression.bodyExpression!!.replace(
            psiFactory.createExpression(
                BLOCK_TEXT
            )
        )
    }

    fun replaceBody(accessor: KtPropertyAccessor) = invokeLater {
        accessor.bodyBlockExpression!!.replace(psiFactory.createBlock(BLOCK_TEXT))
    }

    companion object {
        private const val BLOCKLESS_TEXT = " TODO(\"Removed by DD\")"
        private const val BLOCK_TEXT = "{ $BLOCKLESS_TEXT) }"
    }
}