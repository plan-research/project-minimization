package org.plan.research.minimization.plugin.model

import arrow.core.Either
import org.plan.research.minimization.plugin.errors.CompilationPropertyCheckerError
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.exception.CompilationException

typealias CompilationResult = Either<CompilationPropertyCheckerError, CompilationException>

/**
 * An interface that provides a simple way to acquire the exception from build
 */
interface BuildExceptionProvider {
    suspend fun checkCompilation(context: IJDDContext): CompilationResult
}
