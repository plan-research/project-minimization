package org.plan.research.minimization.plugin.modification.psi.lookup

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.psi.KtCallElement

object ParameterLookup {
    fun lookupFunctionParametersOrder(callObject: KtCallElement) = analyze(callObject) {
        val parameters = lookupFunctionParameters(callObject)
        val arguments = callObject
            .valueArguments
            .mapIndexed { idx, it -> it.getArgumentExpression() to idx }
            .associate { (expr, idx) -> expr to idx }
        parameters
            ?.entries
            ?.sortedBy { (expr) -> arguments[expr]!! }
            ?.map { (_, parameter) -> parameter.name.asString() }
    }

    private fun KaSession.lookupFunctionParameters(callObject: KtCallElement) =
        callObject
            .resolveToCall()
            ?.singleFunctionCallOrNull()
            ?.argumentMapping
}
