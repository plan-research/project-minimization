package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.execution.withTransformations
import org.plan.research.minimization.plugin.getCompilationStrategy
import org.plan.research.minimization.plugin.getExceptionTransformations
import org.plan.research.minimization.plugin.model.BuildExceptionProvider
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.exception.CompilationResult
import org.plan.research.minimization.plugin.model.exception.ExceptionTransformation
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class BuildExceptionProviderService(
    private val initialProject: Project,
) : BuildExceptionProvider {
    private val transformations: List<ExceptionTransformation>
        get() = initialProject
            .service<MinimizationPluginSettings>()
            .state
            .minimizationTransformations
            .map { it.getExceptionTransformations() }

    private val underlyingObject: BuildExceptionProvider
        get() = initialProject
            .service<MinimizationPluginSettings>()
            .state
            .currentCompilationStrategy
            .getCompilationStrategy()
            .withTransformations(transformations)

    override suspend fun checkCompilation(context: IJDDContext): CompilationResult =
        underlyingObject
            .checkCompilation(context)
}
