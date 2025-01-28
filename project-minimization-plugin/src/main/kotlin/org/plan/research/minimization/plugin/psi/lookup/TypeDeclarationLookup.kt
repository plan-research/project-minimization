package org.plan.research.minimization.plugin.psi.lookup

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.analysis.api.types.abbreviationOrSelf
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.psi.*

internal object TypeDeclarationLookup {
    fun getSymbolTypeDeclarations(symbol: PsiElement): List<PsiElement> {
        if (symbol.containingFile !is KtFile) {
            return emptyList()
        }

        return when (symbol) {
            is PsiWhiteSpace -> {
                // Navigate to type of first parameter in lambda, works with the help of KotlinTargetElementEvaluator for the 'it' case
                val lBraceElement = symbol.containingFile.findElementAt(maxOf(symbol.textOffset - 1, 0))
                if (lBraceElement?.text == "{") {
                    (lBraceElement.parent as? KtFunctionLiteral)?.let { return getFunctionalLiteralTarget(it) }
                }
                emptyList()
            }

            is KtFunctionLiteral -> getFunctionalLiteralTarget(symbol)
            is KtTypeReference -> {
                val declaration = symbol.parent
                if (declaration is KtCallableDeclaration && declaration.receiverTypeReference == symbol) {
                    // Navigate to function receiver type, works with the help of KotlinTargetElementEvaluator for the 'this' in extension declaration
                    declaration.getTypeDeclarationFromCallable { callableSymbol -> callableSymbol.receiverType }
                } else {
                    emptyList()
                }
            }

            is KtCallableDeclaration -> symbol.getTypeDeclarationFromCallable { callableSymbol -> callableSymbol.returnType }
            is KtClassOrObject -> getClassTypeDeclaration(symbol)
            is KtTypeAlias -> getTypeAliasDeclaration(symbol)
            else -> emptyList()
        }
    }

    private fun getFunctionalLiteralTarget(symbol: KtFunctionLiteral): List<PsiElement> =
        symbol.getTypeDeclarationFromCallable { callableSymbol ->
            (callableSymbol as? KaFunctionSymbol)?.valueParameters?.firstOrNull()?.returnType
                ?: callableSymbol.receiverType
        } + symbol.getParametersTypeDeclarationsFromCallable { callableSymbol ->
            (callableSymbol as? KaFunctionSymbol)?.valueParameters?.drop(1)?.map { it.returnType } ?: emptyList()
        }

    private fun getClassTypeDeclaration(symbol: KtClassOrObject): List<PsiElement> =
        analyze(symbol) {
            listOfNotNull((symbol.symbol as? KaNamedClassSymbol)?.psi)
        }

    private fun getTypeAliasDeclaration(symbol: KtTypeAlias): List<PsiElement> =
        analyze(symbol) {
            val typeAliasSymbol = symbol.symbol as? KaTypeAliasSymbol
            listOfNotNull((typeAliasSymbol?.expandedType?.expandedSymbol as? KaNamedClassSymbol)?.psi)
        }

    private fun KtCallableDeclaration.getTypeDeclarationFromCallable(typeFromSymbol: (KaCallableSymbol) -> KaType?): List<PsiElement> =
        if (this is KtParameter && this.isFunctionTypeParameter) {
            emptyList()
        } else {
            analyze(this) {
                val symbol = symbol as? KaCallableSymbol ?: return emptyList()
                val type = typeFromSymbol(symbol) ?: return emptyList()
                val targetSymbol = type.upperBoundIfFlexible().abbreviationOrSelf.getSymbol() ?: return emptyList()
                listOfNotNull(targetSymbol.psi)
            }
        }

    private fun KtCallableDeclaration.getParametersTypeDeclarationsFromCallable(typeFromSymbol: (KaCallableSymbol) -> List<KaType?>): List<PsiElement> =
        analyze(this) {
            val symbol = symbol as? KaCallableSymbol ?: return emptyList()
            val types = typeFromSymbol(symbol)
            types.mapNotNull {
                it?.upperBoundIfFlexible()?.abbreviationOrSelf?.symbol
                    ?.psi
            }
        }

    private fun KaType.getSymbol(): KaSymbol? = when (this) {
        is KaTypeParameterType -> symbol
        is KaUsualClassType -> symbol
        else -> symbol
    }
}
