package org.plan.research.minimization.plugin.compilation.comparator

import org.plan.research.minimization.plugin.compilation.exception.CompilationException

class SimpleExceptionComparator : ExceptionComparator {
    override suspend fun areEquals(exception1: CompilationException, exception2: CompilationException) =
        exception1 == exception2
}
