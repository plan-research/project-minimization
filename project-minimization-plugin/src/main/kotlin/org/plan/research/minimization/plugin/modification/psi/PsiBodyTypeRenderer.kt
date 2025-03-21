package org.plan.research.minimization.plugin.modification.psi

import org.plan.research.minimization.plugin.modification.item.PsiChildrenIndexDDItem
import org.plan.research.minimization.plugin.modification.psi.KotlinTypeRenderer.renderType

import com.intellij.psi.impl.source.PsiMethodImpl
import com.intellij.psi.impl.source.tree.java.PsiLambdaExpressionImpl
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPropertyAccessor

object PsiBodyTypeRenderer : PsiChildrenIndexDDItem.PsiWithBodyTransformer<String?> {
    override fun transform(classInitializer: KtClassInitializer): String? = null

    override fun transform(function: KtNamedFunction): String =
        analyze(function) {
            val type = function.returnType
            renderType(function.containingKtFile, type)
        }

    override fun transform(lambdaExpression: KtLambdaExpression): String? =
        analyze(lambdaExpression) {
            val type = (lambdaExpression.expressionType as? KaFunctionType)?.returnType ?: return null
            renderType(lambdaExpression.containingKtFile, type)
        }

    override fun transform(accessor: KtPropertyAccessor): String =
        analyze(accessor) {
            val type = accessor.returnType
            renderType(accessor.containingKtFile, type)
        }

    // Rendered type is unused for java PSI
    override fun transform(method: PsiMethodImpl): String? = null
    override fun transform(lambdaExpression: PsiLambdaExpressionImpl): String? = null
}
