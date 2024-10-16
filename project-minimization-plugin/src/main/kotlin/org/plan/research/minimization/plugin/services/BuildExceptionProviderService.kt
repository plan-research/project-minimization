package org.plan.research.minimization.plugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.plan.research.minimization.plugin.getCompilationStrategy
import org.plan.research.minimization.plugin.model.BuildExceptionProvider
import org.plan.research.minimization.plugin.model.exception.CompilationResult
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings

@Service(Service.Level.PROJECT)
class BuildExceptionProviderService(
    private val initialProject: Project,
) : BuildExceptionProvider {
    private val underlyingObject: BuildExceptionProvider
        get() = initialProject
            .service<MinimizationPluginSettings>()
            .state.currentCompilationStrategy
            .getCompilationStrategy()

    override suspend fun checkCompilation(project: Project): CompilationResult =
        underlyingObject
            .checkCompilation(project)
}