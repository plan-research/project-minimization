package org.plan.research.minimization.plugin.model.exception

import org.plan.research.minimization.plugin.errors.CompilationPropertyCheckerError
import org.plan.research.minimization.plugin.model.IJDDContext

import arrow.core.Either

typealias CompilationResult = Either<CompilationPropertyCheckerError, CompilationException>

interface CompilationException {
    suspend fun apply(transformation: ExceptionTransformation, context: IJDDContext): CompilationException
}
