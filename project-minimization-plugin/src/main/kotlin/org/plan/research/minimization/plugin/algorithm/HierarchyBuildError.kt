package org.plan.research.minimization.plugin.algorithm

sealed interface HierarchyBuildError {
    data object NoExceptionFound : HierarchyBuildError
}
