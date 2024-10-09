package org.plan.research.minimization.plugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import org.plan.research.minimization.plugin.getCompilationStrategy
import org.plan.research.minimization.plugin.model.BuildExceptionProvider
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings

@Service(Service.Level.PROJECT)
class BuildExceptionProviderService(
    private val initialProject: Project,
    private val coroutineScope: CoroutineScope
): BuildExceptionProvider {
    private val underlyingObject: BuildExceptionProvider
        get() = initialProject
            .service<MinimizationPluginSettings>()
            .state.currentCompilationStrategy
            .getCompilationStrategy(coroutineScope)

    override suspend fun checkCompilation(project: Project) =
        underlyingObject
            .checkCompilation(project)
}