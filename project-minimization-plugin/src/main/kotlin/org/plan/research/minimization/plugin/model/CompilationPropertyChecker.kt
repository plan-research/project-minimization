package org.plan.research.minimization.plugin.model

import arrow.core.Either
import com.intellij.openapi.project.Project
import org.plan.research.minimization.plugin.errors.CompilationPropertyCheckerError


interface CompilationPropertyChecker {
    suspend fun checkCompilation(project: Project): Either<CompilationPropertyCheckerError, Throwable>
}