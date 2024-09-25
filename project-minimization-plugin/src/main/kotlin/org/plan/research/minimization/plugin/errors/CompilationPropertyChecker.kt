package org.plan.research.minimization.plugin.errors

sealed interface CompilationPropertyCheckerError
data object CompilationSuccess : CompilationPropertyCheckerError
