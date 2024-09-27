package org.plan.research.minimization.plugin.errors

sealed interface HierarchyBuildError

data object NoRootFound : HierarchyBuildError
data object NoExceptionFound : HierarchyBuildError
