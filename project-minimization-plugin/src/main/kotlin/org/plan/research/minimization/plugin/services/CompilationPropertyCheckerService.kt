package org.plan.research.minimization.plugin.services

import arrow.core.Either
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.plan.research.minimization.plugin.errors.CompilationPropertyCheckerError
import org.plan.research.minimization.plugin.model.CompilationPropertyChecker
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings
import java.nio.file.Path

class CompilationPropertyCheckerService(
    private val initialProject: Project
) :
    CompilationPropertyChecker {
    private val underlyingObject: CompilationPropertyChecker
        get() = initialProject
            .service<MinimizationPluginSettings>()
            .state
            .getCompilationStrategy()

    override fun checkCompilation(project: Project): Either<CompilationPropertyCheckerError, Throwable> =
        underlyingObject
            .checkCompilation(project)

    override fun getLastSnapshot(): Path =
        underlyingObject
            .getLastSnapshot()
}