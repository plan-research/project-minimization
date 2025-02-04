package org.plan.research.minimization.plugin.algorithm

sealed interface MinimizationError {
    data object CloningFailed : MinimizationError
    data object PropertyCheckerFailed : MinimizationError
    data object OpeningFailed : MinimizationError
    data object AnalysisFailed : MinimizationError
    data class HierarchyFailed(val error: HierarchyBuildError) : MinimizationError
}
