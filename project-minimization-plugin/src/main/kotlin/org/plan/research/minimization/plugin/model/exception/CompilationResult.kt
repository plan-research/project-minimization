package org.plan.research.minimization.plugin.model.exception

import arrow.core.Either
import org.plan.research.minimization.plugin.errors.CompilationPropertyCheckerError
import org.plan.research.minimization.plugin.model.IJDDContext

interface CompilationException {
    suspend fun apply(transformation: ExceptionTransformation, context: IJDDContext): CompilationException
}
typealias CompilationResult = Either<CompilationPropertyCheckerError, CompilationException>