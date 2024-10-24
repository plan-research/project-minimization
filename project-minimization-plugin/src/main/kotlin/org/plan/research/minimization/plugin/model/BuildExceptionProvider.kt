package org.plan.research.minimization.plugin.model

import org.plan.research.minimization.plugin.model.exception.CompilationResult

/**
 * An interface that provides a simple way to acquire the exception from build
 */
interface BuildExceptionProvider {
    suspend fun checkCompilation(context: IJDDContext): CompilationResult
}
