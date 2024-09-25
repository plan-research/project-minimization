package org.plan.research.minimization.plugin.model

import arrow.core.Either
import com.intellij.openapi.project.Project
import org.plan.research.minimization.plugin.errors.CompilationPropertyCheckerError
import java.nio.file.Path


interface CompilationPropertyChecker {
    fun checkCompilation(project: Project): Either<CompilationPropertyCheckerError, Throwable>
    fun getLastSnapshot(): Path /* FIXME */
}