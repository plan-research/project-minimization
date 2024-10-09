package org.plan.research.minimization.plugin.model

import arrow.core.Either
import org.plan.research.minimization.plugin.errors.CompilationPropertyCheckerError

interface CompilationException
typealias CompilationResult = Either<CompilationPropertyCheckerError, CompilationException>