package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.compilation.BuildExceptionProvider
import org.plan.research.minimization.plugin.compilation.CompilationResult
import org.plan.research.minimization.plugin.compilation.transformer.ExceptionTransformer
import org.plan.research.minimization.plugin.compilation.transformer.withTransformers
import org.plan.research.minimization.plugin.context.IJDDContext
import org.plan.research.minimization.plugin.util.getCompilationStrategy
import org.plan.research.minimization.plugin.util.getExceptionTransformations

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class BuildExceptionProviderService(
    private val initialProject: Project,
) : BuildExceptionProvider {
    private val transformations: List<ExceptionTransformer> by initialProject
        .service<MinimizationPluginSettings>()
        .stateObservable
        .minimizationTransformations
        .observe { transList -> transList.map { it.getExceptionTransformations() } }
    private val underlyingObject: BuildExceptionProvider by initialProject
        .service<MinimizationPluginSettings>()
        .stateObservable
        .compilationStrategy
        .observe { it.getCompilationStrategy().withTransformers(transformations) }

    override suspend fun checkCompilation(context: IJDDContext): CompilationResult =
        underlyingObject
            .checkCompilation(context)
}
