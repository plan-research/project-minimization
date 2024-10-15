package org.plan.research.minimization.plugin.model.exception

/**
 * An interface for comparing two instances of [CompilationException].
 */
interface ExceptionComparator {
    fun areEquals(exception1: CompilationException, exception2: CompilationException): Boolean
}