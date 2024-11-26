package org.plan.research.minimization.plugin.psi

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.psi
import org.jetbrains.kotlin.analysis.api.symbols.sourcePsi
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty

object KotlinOverriddenElementsGetter {
    fun getOverriddenElements(element: KtElement): List<KtElement> = when (element) {
        is KtNamedFunction -> getOverriddenFunction(element)
        is KtProperty -> getOverriddenProperties(element)
        else -> emptyList()
    }

    private fun getOverriddenFunction(element: KtNamedFunction) = analyze(element) {
        val symbol = element.symbol
        symbol
            .directlyOverriddenSymbols
            .mapNotNull { it.sourcePsi() }
            .filterIsInstance<KtElement>()
            .toList()
    }

    private fun getOverriddenProperties(element: KtProperty) = analyze(element) {
        val symbol = element.symbol
        symbol
            .directlyOverriddenSymbols
            .mapNotNull { it.sourcePsi() }
            .filterIsInstance<KtElement>()
            .toList()
    }
}
