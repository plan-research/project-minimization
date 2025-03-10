package org.plan.research.minimization.plugin.modification.psi.imports

import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.resolution.KaCallableMemberCall
import org.jetbrains.kotlin.analysis.api.resolution.KaExplicitReceiverValue
import org.jetbrains.kotlin.analysis.api.resolution.KaImplicitReceiverValue
import org.jetbrains.kotlin.analysis.api.resolution.KaReceiverValue
import org.jetbrains.kotlin.analysis.api.resolution.KaSimpleFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.KaSmartCastedReceiverValue
import org.jetbrains.kotlin.analysis.api.resolution.singleCallOrNull
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.name
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.idea.references.KDocReference
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.withClassId
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression

internal data class UsedSymbol(val reference: KtReference, val symbol: KaSymbol) {
    fun KaSession.computeImportableFqName(): FqName? = computeImportableName(symbol, resolveDispatchReceiver(reference.element) as? KaImplicitReceiverValue?)

    fun KaSession.isResolvedWithImport(): Boolean {
        val isNotAliased = symbol.name in reference.resolvesByNames

        if (isNotAliased && isAccessibleAsMemberCallable(symbol, reference.element)) {
            return false
        }
        if (isNotAliased && isAccessibleAsMemberClassifier(symbol, reference.element)) {
            return false
        }

        return canBeResolvedViaImport(reference, symbol)
    }
}

@Suppress("FUNCTION_BOOLEAN_PREFIX")
internal fun KaSession.typeIsPresentAsImplicitReceiver(
    type: KaType,
    contextPosition: KtElement,
): Boolean {
    val containingFile = contextPosition.containingKtFile
    val implicitReceivers = containingFile.scopeContext(contextPosition).implicitReceivers

    return implicitReceivers.any { it.type.semanticallyEquals(type) }
}

private fun KaSession.resolveDispatchReceiver(element: KtElement): KaReceiverValue? {
    val adjustedElement = element.callableReferenceExpressionForCallableReference() ?: element
    val dispatchReceiver = adjustedElement.resolveToCall()
        ?.singleCallOrNull<KaCallableMemberCall<*, *>>()
        ?.partiallyAppliedSymbol
        ?.dispatchReceiver

    return dispatchReceiver
}

private fun KaSession.isAccessibleAsMemberCallable(
    symbol: KaSymbol,
    element: KtElement,
): Boolean {
    if (symbol !is KaCallableSymbol || containingDeclarationPatched(symbol) !is KaClassLikeSymbol) {
        return false
    }

    if (symbol is KaEnumEntrySymbol) {
        return isAccessibleAsMemberCallableDeclaration(symbol, element)
    }

    val dispatchReceiver = resolveDispatchReceiver(element) ?: return false

    return isDispatchedCall(element, symbol, dispatchReceiver)
}

private fun KtElement.callableReferenceExpressionForCallableReference(): KtCallableReferenceExpression? =
    (parent as? KtCallableReferenceExpression)?.takeIf { it.callableReference == this }

private fun KaSession.isDispatchedCall(
    element: KtElement,
    symbol: KaCallableSymbol,
    dispatchReceiver: KaReceiverValue,
): Boolean = when (dispatchReceiver) {
    is KaExplicitReceiverValue -> true

    is KaSmartCastedReceiverValue -> isDispatchedCall(element, symbol, dispatchReceiver.original)

    is KaImplicitReceiverValue -> !isStaticallyImportedReceiver(element, symbol, dispatchReceiver)
}

private fun KaSession.isStaticallyImportedReceiver(
    element: KtElement,
    symbol: KaCallableSymbol,
    implicitDispatchReceiver: KaImplicitReceiverValue,
): Boolean {
    val receiverTypeSymbol = implicitDispatchReceiver.type.symbol ?: return false
    val receiverIsObject = receiverTypeSymbol is KaClassSymbol && receiverTypeSymbol.classKind.isObject

    // with static imports, the implicit receiver is either some object symbol or `Unit` in case of imports from Java classes
    if (!receiverIsObject) {
        return false
    }

    return if (symbol.isJavaStaticDeclaration()) {
        !isAccessibleAsMemberCallableDeclaration(symbol, element)
    } else {
        !typeIsPresentAsImplicitReceiver(implicitDispatchReceiver.type, element)
    }
}

private fun KaSession.resolveExtensionReceiverForFunctionalTypeVariable(
    referenceExpression: KtNameReferenceExpression?,
    target: KaSymbol,
): KaExplicitReceiverValue? {
    val parentCall = referenceExpression?.parent as? KtCallExpression
    val isFunctionalTypeVariable = target is KaPropertySymbol && target.returnType.let { it.isFunctionType || it.isSuspendFunctionType }

    if (parentCall == null || !isFunctionalTypeVariable) {
        return null
    }

    val parentCallInfo = parentCall.resolveToCall()?.singleCallOrNull<KaSimpleFunctionCall>() ?: return null
    if (!parentCallInfo.isImplicitInvoke) {
        return null
    }

    return parentCallInfo.partiallyAppliedSymbol.extensionReceiver as? KaExplicitReceiverValue
}

private fun KaSession.canBeResolvedViaImport(reference: KtReference, target: KaSymbol): Boolean {
    if (reference is KDocReference) {
        return canBeResolvedViaImport(reference, target)
    }

    if (target is KaCallableSymbol && target.isExtension) {
        return true
    }

    val referenceExpression = reference.element as? KtNameReferenceExpression

    val explicitReceiver = referenceExpression?.getReceiverExpression()
        ?: referenceExpression?.callableReferenceExpressionForCallableReference()?.receiverExpression

    explicitReceiver?.let {
        val extensionReceiver = resolveExtensionReceiverForFunctionalTypeVariable(referenceExpression, target)
        return extensionReceiver?.expression == explicitReceiver
    }

    return true
}

@OptIn(KaIdeApi::class)
private fun KaSession.computeImportableName(
    target: KaSymbol,
    implicitDispatchReceiver: KaImplicitReceiverValue?,  // TODO: support other types of dispatcher values
): FqName? {
    implicitDispatchReceiver ?: return target.importableFqName

    if (target !is KaCallableSymbol) {
        return null
    }

    val callableId = target.callableId ?: return null
    callableId.classId ?: return null

    val implicitReceiver = implicitDispatchReceiver.symbol as? KaClassLikeSymbol ?: return null
    val implicitReceiverClassId = implicitReceiver.classId ?: return null

    val substitutedCallableId = callableId.withClassId(implicitReceiverClassId)

    return substitutedCallableId.asSingleFqName()
}

private fun KaSession.canBeResolvedViaImport(reference: KDocReference, target: KaSymbol): Boolean {
    val qualifier = reference.element.getQualifier() ?: return true

    return if (target is KaCallableSymbol && target.isExtension) {
        val elementHasFunctionDescriptor = reference.element.mainReference.resolveToSymbols()
            .any { it is KaFunctionSymbol }
        val qualifierHasClassDescriptor = qualifier.mainReference.resolveToSymbols().any { it is KaClassLikeSymbol }
        elementHasFunctionDescriptor && qualifierHasClassDescriptor
    } else {
        false
    }
}
