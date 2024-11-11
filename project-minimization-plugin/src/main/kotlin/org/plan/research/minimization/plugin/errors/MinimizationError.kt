package org.plan.research.minimization.plugin.errors

import org.plan.research.minimization.plugin.prototype.slicing.SlicingServiceError

sealed interface MinimizationError {
    data object CloningFailed : MinimizationError
    data object PropertyCheckerFailed : MinimizationError
    data class HierarchyFailed(val error: HierarchyBuildError) : MinimizationError
    data class SlicingFailed(val error: SlicingServiceError): MinimizationError
}
