package org.plan.research.minimization.plugin.compilation.comparator

import org.plan.research.minimization.plugin.compilation.exception.CompilationException

/**
 * An interface for comparing two instances of [CompilationException].
 */
interface ExceptionComparator {
    suspend fun areEquals(exception1: CompilationException, exception2: CompilationException): Boolean
}
