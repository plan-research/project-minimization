package org.plan.research.minimization.plugin.compilation

sealed interface CompilationPropertyCheckerError {
    data object CompilationSuccess : CompilationPropertyCheckerError
    data object InvalidBuildSystem : CompilationPropertyCheckerError
    data class BuildSystemFail(val cause: Throwable) : CompilationPropertyCheckerError
}
