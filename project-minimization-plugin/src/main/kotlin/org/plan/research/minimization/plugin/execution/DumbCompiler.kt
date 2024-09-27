package org.plan.research.minimization.plugin.execution

import arrow.core.Either
import arrow.core.right
import com.intellij.openapi.project.Project
import org.plan.research.minimization.plugin.errors.CompilationPropertyCheckerError
import org.plan.research.minimization.plugin.model.CompilationPropertyChecker

object DumbCompiler : CompilationPropertyChecker {
    override suspend fun checkCompilation(project: Project): Either<CompilationPropertyCheckerError, Throwable> =
        THROWABLE.right()

    private val THROWABLE = Throwable()
}