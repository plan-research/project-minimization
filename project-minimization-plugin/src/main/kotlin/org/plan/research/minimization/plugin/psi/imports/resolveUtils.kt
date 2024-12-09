package org.plan.research.minimization.plugin.psi.imports

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaScopeKind
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaJavaFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSamConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory

internal fun KaSession.resolveTypeAliasedConstructorReference(
    reference: KtReference,
    expandedClassSymbol: KaClassLikeSymbol,
    containingFile: KtFile,
): KaClassLikeSymbol? {
    val originalReferenceName = reference.resolvesByNames.singleOrNull() ?: return null

    // optimization to avoid resolving typealiases which are not available
    if (!typeAliasIsAvailable(originalReferenceName, containingFile)) {
        return null
    }

    val referencedType = resolveReferencedType(reference) ?: return null
    if (referencedType.symbol != expandedClassSymbol) {
        return null
    }

    val typealiasType = referencedType.abbreviatedType ?: return null

    return typealiasType.symbol
}

internal fun KaSession.containingDeclarationPatched(symbol: KaSymbol): KaDeclarationSymbol? {
    symbol.containingDeclaration?.let { return it }

    val declarationPsi = symbol.psi

    if (declarationPsi is PsiMember) {
        val containingClass = declarationPsi.parent as? PsiClass
        containingClass?.namedClassSymbol?.let { return it }
    }

    return null
}

internal fun KaSession.isAccessibleAsMemberCallableDeclaration(
    symbol: KaCallableSymbol,
    contextPosition: KtElement,
): Boolean {
    if (containingDeclarationPatched(symbol) !is KaClassLikeSymbol) {
        return false
    }

    if (symbol !is KaNamedSymbol) {
        return false
    }

    val nonImportingScopes = nonImportingScopesForPosition(contextPosition).asCompositeScope()

    return nonImportingScopes.callables(symbol.name).any { it == symbol }
}

internal fun KaSession.findSamClassFor(samConstructorSymbol: KaSamConstructorSymbol): KaClassSymbol? {
    val samCallableId = samConstructorSymbol.callableId ?: return null
    if (samCallableId.isLocal) {
        return null
    }

    val samClassId = ClassId.fromString(samCallableId.toString())

    return findClass(samClassId)
}

internal fun KaCallableSymbol.isJavaStaticDeclaration(): Boolean =
    when (this) {
        is KaNamedFunctionSymbol -> isStatic
        is KaPropertySymbol -> isStatic
        is KaJavaFieldSymbol -> isStatic
        else -> false
    }

internal fun KaSession.isAccessibleAsMemberClassifier(symbol: KaSymbol, element: KtElement): Boolean {
    if (symbol !is KaClassLikeSymbol || containingDeclarationPatched(symbol) !is KaClassLikeSymbol) {
        return false
    }

    val name = symbol.name ?: return false

    val nonImportingScopes = nonImportingScopesForPosition(element).asCompositeScope()

    val foundClasses = nonImportingScopes.classifiers(name)
    val foundClass = foundClasses.firstOrNull()

    return symbol == foundClass
}

@Suppress("FUNCTION_BOOLEAN_PREFIX")
private fun KaSession.typeAliasIsAvailable(name: Name, containingFile: KtFile): Boolean {
    val importingScope = containingFile.importingScopeContext
    val foundClassifiers = importingScope.compositeScope().classifiers(name)

    return foundClassifiers.any { it is KaTypeAliasSymbol }
}

private fun KaSession.resolveReferencedType(reference: KtReference): KaType? {
    val originalReferenceName = reference.resolvesByNames.singleOrNull() ?: return null

    val psiFactory = KtPsiFactory.contextual(reference.element)
    val psiType = psiFactory.createTypeCodeFragment(originalReferenceName.asString(), context = reference.element).getContentElement()

    return psiType?.type
}

private fun KaSession.nonImportingScopesForPosition(element: KtElement): List<KaScope> {
    val scopeContext = element.containingKtFile.scopeContext(element)

    // we have to filter scopes created by implicit receivers (like companion objects, for example); see KT-70108
    val implicitReceiverScopeIndices = scopeContext.implicitReceivers.map { it.scopeIndexInTower }.toSet()

    val nonImportingScopes = scopeContext.scopes
        .asSequence()
        .filterNot { it.kind is KaScopeKind.ImportingScope }
        .filterNot { it.kind.indexInTower in implicitReceiverScopeIndices }
        .map { it.scope }
        .toList()

    return nonImportingScopes
}
