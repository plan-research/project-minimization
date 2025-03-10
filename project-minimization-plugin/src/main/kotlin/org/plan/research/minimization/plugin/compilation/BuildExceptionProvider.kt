package org.plan.research.minimization.plugin.compilation

import org.plan.research.minimization.plugin.compilation.exception.CompilationException
import org.plan.research.minimization.plugin.context.IJDDContext

import arrow.core.Either

typealias CompilationResult = Either<CompilationPropertyCheckerError, CompilationException>

/**
 * An interface that provides a simple way to acquire the exception from build
 */
interface BuildExceptionProvider {
    suspend fun checkCompilation(context: IJDDContext): CompilationResult
}
