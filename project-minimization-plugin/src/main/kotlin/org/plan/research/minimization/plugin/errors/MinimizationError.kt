package org.plan.research.minimization.plugin.errors

sealed interface MinimizationError {
    data class HierarchyFailed(val error: HierarchyBuildError) : MinimizationError
}