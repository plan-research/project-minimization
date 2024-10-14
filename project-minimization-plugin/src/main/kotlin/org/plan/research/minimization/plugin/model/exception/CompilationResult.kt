package org.plan.research.minimization.plugin.model.exception

import arrow.core.Either
import arrow.core.Option
import org.plan.research.minimization.plugin.errors.CompilationPropertyCheckerError

interface CompilationException {
    suspend fun transformBy(transformation: ExceptionTransformation): Option<CompilationException>
}
typealias CompilationResult = Either<CompilationPropertyCheckerError, CompilationException>