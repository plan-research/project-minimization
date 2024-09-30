package org.plan.research.minimization.plugin.model.dd

import arrow.core.Either
import com.intellij.openapi.project.Project
import org.plan.research.minimization.plugin.errors.CompilationPropertyCheckerError

/**
 * An interface that provides a simple way to acquire the exception from build
 */
interface CompilationPropertyChecker {
    suspend fun checkCompilation(project: Project): Either<CompilationPropertyCheckerError, Throwable>
}