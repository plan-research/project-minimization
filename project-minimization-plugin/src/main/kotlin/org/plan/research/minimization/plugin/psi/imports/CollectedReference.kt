package org.plan.research.minimization.plugin.psi.imports

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.resolution.KaSimpleFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.calls
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSamConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.idea.references.KDocReference
import org.jetbrains.kotlin.idea.references.KtDefaultAnnotationArgumentReference
import org.jetbrains.kotlin.idea.references.KtInvokeFunctionReference
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.psi.psiUtil.unwrapParenthesesLabelsAndAnnotations

internal data class CollectedReference(val reference: KtReference) {
    fun KaSession.resolvesByNames(): Collection<Name> {
        if (reference is KDocReference && !isResolved()) {
            // if KDoc reference is unresolved, do not consider it to be an unresolved symbol (see KT-61785)
            return emptyList()
        }

        return reference.resolvesByNames
    }

    fun KaSession.isResolved(): Boolean {
        if (reference is KtInvokeFunctionReference) {
            // invoke references on Kotlin builtin functional types (like `() -> Unit`)
            // always have empty `resolveToSymbols`, so we have to do the check another way
            val callInfo = reference.element.resolveToCall() ?: return false

            return callInfo.calls.isNotEmpty()
        }

        val resolvedSymbols = reference.resolveToSymbols()

        return resolvedSymbols.isNotEmpty()
    }

    fun KaSession.resolveToImportableSymbols(): Collection<UsedSymbol> = reference
        .resolveToSymbols()
        .mapNotNull { toImportableSymbol(it, reference) }
        .map { UsedSymbol(reference, it) }
    companion object {
        fun KaSession.createFrom(reference: KtReference): CollectedReference? = when {
            reference is KtDefaultAnnotationArgumentReference -> null
            isUnaryOperatorOnIntLiteralReference(reference) -> null
            isEmptyInvokeReference(reference) -> null
            else -> CollectedReference(reference)
        }
    }
}

private fun KaSession.toImportableSymbol(
    target: KaSymbol,
    reference: KtReference,
    containingFile: KtFile = reference.element.containingKtFile,
): KaSymbol? = when {
    target is KaReceiverParameterSymbol -> null

    reference.isImplicitReferenceToCompanion() -> (target as? KaNamedClassSymbol)?.containingSymbol

    target is KaConstructorSymbol -> {
        val targetClass = target.containingSymbol as? KaClassLikeSymbol

        // if constructor is typealiased, it can be imported in any scenario
        val typeAlias = targetClass?.let { resolveTypeAliasedConstructorReference(reference, it, containingFile) }

        // if constructor leads to inner class, it cannot be resolved by import
        val notInnerTargetClass = targetClass?.takeUnless { it is KaNamedClassSymbol && it.isInner }

        typeAlias ?: notInnerTargetClass
    }

    target is KaSamConstructorSymbol -> {
        val targetClass = findSamClassFor(target)

        targetClass?.let { resolveTypeAliasedConstructorReference(reference, it, containingFile) } ?: targetClass
    }

    else -> target
}

/**
 * In K2, every call in the form of `foo()` has `KtInvokeFunctionReference` on it.
 *
 * In the cases when `foo()` call is not actually an `invoke` call, we do not want to process such references,
 * since they are not supposed to resolve anywhere.
 */
private fun KaSession.isEmptyInvokeReference(reference: KtReference): Boolean {
    if (reference !is KtInvokeFunctionReference) {
        return false
    }

    val callInfo = reference.element.resolveToCall()
    val isImplicitInvoke = callInfo?.calls?.any { it is KaSimpleFunctionCall && it.isImplicitInvoke } == true

    return !isImplicitInvoke
}

private fun isUnaryOperatorOnIntLiteralReference(reference: KtReference): Boolean {
    val unaryOperationReferenceExpression = reference.element as? KtOperationReferenceExpression ?: return false

    if (unaryOperationReferenceExpression.operationSignTokenType !in arrayOf(KtTokens.PLUS, KtTokens.MINUS)) {
        return false
    }

    val prefixExpression = unaryOperationReferenceExpression.parent as? KtUnaryExpression ?: return false
    val unwrappedBaseExpression = prefixExpression.baseExpression?.unwrapParenthesesLabelsAndAnnotations() ?: return false

    return unwrappedBaseExpression is KtConstantExpression &&
        unwrappedBaseExpression.elementType == KtNodeTypes.INTEGER_CONSTANT
}
