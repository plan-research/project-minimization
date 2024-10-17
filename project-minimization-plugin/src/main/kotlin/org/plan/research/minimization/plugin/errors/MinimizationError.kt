package org.plan.research.minimization.plugin.errors

sealed interface MinimizationError {
    data object CloningFailed : MinimizationError

    /**
     * @property error
     */
    data class HierarchyFailed(val error: HierarchyBuildError) : MinimizationError
}
