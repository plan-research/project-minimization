package org.plan.research.minimization.plugin.model

import com.intellij.openapi.project.Project

/**
 * An interface that provides a simple way to acquire the exception from build
 */
interface BuildExceptionProvider {
    suspend fun checkCompilation(project: Project): CompilationResult
}