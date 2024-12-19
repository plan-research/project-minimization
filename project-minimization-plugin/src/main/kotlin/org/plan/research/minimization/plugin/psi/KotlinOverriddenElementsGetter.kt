package org.plan.research.minimization.plugin.psi

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.psi
import org.jetbrains.kotlin.analysis.api.symbols.sourcePsi
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty

object KotlinOverriddenElementsGetter {
    fun getOverriddenElements(element: KtElement): List<KtElement> = when (element) {
        is KtNamedFunction -> getOverriddenFunction(element)
        is KtProperty -> getOverriddenProperties(element)
        is KtClass -> getOverriddenClass(element) // JBRes-2212
        is KtParameter -> getOverriddenParameters(element)
        else -> emptyList()
    }

    private fun getOverriddenFunction(element: KtNamedFunction) = analyze(element) {
        val symbol = element.symbol
        symbol
            .directlyOverriddenSymbols
            .map { it.psi<KtElement>() }
            .toList()
    }

    private fun getOverriddenProperties(element: KtProperty) = analyze(element) {
        val symbol = element.symbol
        symbol
            .directlyOverriddenSymbols
            .map { it.psi<KtElement>() }
            .toList()
    }

    private fun getOverriddenParameters(element: KtParameter) = analyze(element) {
        val symbol = element.symbol
        symbol
            .directlyOverriddenSymbols
            .map { it.psi<KtElement>() }
            .toList()
    }
    private fun getOverriddenClass(element: KtClass) = analyze(element) {
        val symbol = element.classSymbol
        symbol
            ?.superTypes
            ?.mapNotNull { it.symbol?.psi<KtElement>() }
            ?.toList()
            ?: emptyList()
    }
}
