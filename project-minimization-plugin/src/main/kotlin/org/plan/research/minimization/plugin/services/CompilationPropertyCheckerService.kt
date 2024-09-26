package org.plan.research.minimization.plugin.services

import arrow.core.Either
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.plan.research.minimization.plugin.errors.CompilationPropertyCheckerError
import org.plan.research.minimization.plugin.getCompilationStrategy
import org.plan.research.minimization.plugin.model.CompilationPropertyChecker
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings

@Service(Service.Level.PROJECT)
class CompilationPropertyCheckerService(
    private val initialProject: Project
): CompilationPropertyChecker {
    private val underlyingObject: CompilationPropertyChecker
        get() = initialProject
            .service<MinimizationPluginSettings>()
            .state.currentCompilationStrategy
            .getCompilationStrategy()

    override suspend fun checkCompilation(project: Project): Either<CompilationPropertyCheckerError, Throwable> =
        underlyingObject
            .checkCompilation(project)
}