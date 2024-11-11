package org.plan.research.minimization.plugin.errors

sealed interface MinimizationError {
    data object CloningFailed : MinimizationError
    data object PropertyCheckerFailed : MinimizationError
    data object OpeningFailed : MinimizationError
    data class HierarchyFailed(val error: HierarchyBuildError) : MinimizationError
}
