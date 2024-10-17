package org.plan.research.minimization.plugin.model

import com.intellij.openapi.project.Project
import org.plan.research.minimization.plugin.model.exception.CompilationResult

/**
 * An interface that provides a simple way to acquire the exception from build
 */
interface BuildExceptionProvider {
    suspend fun checkCompilation(project: Project): CompilationResult
}