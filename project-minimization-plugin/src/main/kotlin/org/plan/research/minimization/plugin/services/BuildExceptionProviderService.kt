package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.execution.withTransformations
import org.plan.research.minimization.plugin.getCompilationStrategy
import org.plan.research.minimization.plugin.getExceptionTransformations
import org.plan.research.minimization.plugin.model.BuildExceptionProvider
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.exception.ExceptionTransformation

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.plan.research.minimization.plugin.model.CompilationResult

@Service(Service.Level.PROJECT)
class BuildExceptionProviderService(
    private val initialProject: Project,
) : BuildExceptionProvider {
    private val transformations: List<ExceptionTransformation> by initialProject
        .service<MinimizationPluginSettings>()
        .stateObservable
        .minimizationTransformations
        .observe { transList -> transList.map { it.getExceptionTransformations() } }
    private val underlyingObject: BuildExceptionProvider by initialProject
        .service<MinimizationPluginSettings>()
        .stateObservable
        .compilationStrategy
        .observe { it.getCompilationStrategy().withTransformations(transformations) }

    override suspend fun checkCompilation(context: IJDDContext): CompilationResult =
        underlyingObject
            .checkCompilation(context)
}
