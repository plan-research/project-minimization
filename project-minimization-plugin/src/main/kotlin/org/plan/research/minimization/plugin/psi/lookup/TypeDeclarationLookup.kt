package org.plan.research.minimization.plugin.psi.lookup

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.idea.intentions.ImportAllMembersIntention.Holder.importReceiverMembers
import org.jetbrains.kotlin.idea.references.canBeResolvedViaImport
import org.jetbrains.kotlin.idea.search.ExpectActualUtils.actualsForExpected
import org.jetbrains.kotlin.idea.search.ExpectActualUtils.expectedDeclarationIfAny
import org.jetbrains.kotlin.psi.*

internal object TypeDeclarationLookup {
    @RequiresReadLock
    fun getSymbolTypeDeclarations(symbol: PsiElement): PsiElement? {
        if (symbol.containingFile !is KtFile) return null

        return when (symbol) {
            is PsiWhiteSpace -> {
                // Navigate to type of first parameter in lambda, works with the help of KotlinTargetElementEvaluator for the 'it' case
                val lBraceElement = symbol.containingFile.findElementAt(maxOf(symbol.textOffset - 1, 0))
                if (lBraceElement?.text == "{") {
                    (lBraceElement.parent as? KtFunctionLiteral)?.let { return getFunctionalLiteralTarget(it) }
                }
                null
            }

            is KtFunctionLiteral -> getFunctionalLiteralTarget(symbol)
            is KtTypeReference -> {
                val declaration = symbol.parent
                if (declaration is KtCallableDeclaration && declaration.receiverTypeReference == symbol) {
                    // Navigate to function receiver type, works with the help of KotlinTargetElementEvaluator for the 'this' in extension declaration
                    declaration.getTypeDeclarationFromCallable { callableSymbol -> callableSymbol.receiverType }
                } else null
            }

            is KtCallableDeclaration -> symbol.getTypeDeclarationFromCallable { callableSymbol -> callableSymbol.returnType }
            is KtClassOrObject -> getClassTypeDeclaration(symbol)
            is KtTypeAlias -> getTypeAliasDeclaration(symbol)
            else -> null
        }
    }

    private fun getFunctionalLiteralTarget(symbol: KtFunctionLiteral): PsiElement? {
        return symbol.getTypeDeclarationFromCallable { callableSymbol ->
            (callableSymbol as? KaFunctionSymbol)?.valueParameters?.firstOrNull()?.returnType
                ?: callableSymbol.receiverType
        }
    }

    @RequiresReadLock
    private fun getClassTypeDeclaration(symbol: KtClassOrObject): PsiElement? {
        analyze(symbol) {
            return (symbol.symbol as? KaNamedClassSymbol)?.psi()
        }
    }

    @RequiresReadLock
    private fun getTypeAliasDeclaration(symbol: KtTypeAlias): PsiElement? {
        analyze(symbol) {
            val typeAliasSymbol = symbol.symbol
            return (typeAliasSymbol.expandedType.expandedSymbol as? KaNamedClassSymbol)?.psi()
        }
    }
    @RequiresReadLock
    private fun KtCallableDeclaration.getTypeDeclarationFromCallable(typeFromSymbol: (KaCallableSymbol) -> KaType?): PsiElement? {
        analyze(this) {
            val symbol = symbol as? KaCallableSymbol ?: return null
            val type = typeFromSymbol(symbol) ?: return null
            val targetSymbol = type.upperBoundIfFlexible().abbreviationOrSelf.symbol ?: return null
            return targetSymbol.psi()
        }
    }

    private val KaType.abbreviationOrSelf: KaType
        get() = abbreviatedType ?: this
}