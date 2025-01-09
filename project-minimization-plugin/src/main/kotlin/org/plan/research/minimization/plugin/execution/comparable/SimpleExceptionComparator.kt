package org.plan.research.minimization.plugin.execution.comparable

import org.plan.research.minimization.plugin.model.exception.CompilationException
import org.plan.research.minimization.plugin.model.exception.ExceptionComparator

class SimpleExceptionComparator : ExceptionComparator {
    override suspend fun areEquals(exception1: CompilationException, exception2: CompilationException) =
        exception1 == exception2
}
