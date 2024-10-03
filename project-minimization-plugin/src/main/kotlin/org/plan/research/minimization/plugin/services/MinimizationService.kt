package org.plan.research.minimization.plugin.services

import arrow.core.raise.either
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import org.plan.research.minimization.plugin.errors.MinimizationError
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings

@Service(Service.Level.PROJECT)
class MinimizationService(project: Project, private val coroutineScope: CoroutineScope) {
    private val stages = project.service<MinimizationPluginSettings>().state.stages
    private val executor = project.service<MinimizationStageExecutorService>()
    private val projectCloning = project.service<ProjectCloningService>()

    fun minimizeProject(project: Project) = coroutineScope.async {
        either {
            val clonedProject = projectCloning.clone(project)
                ?: raise(MinimizationError.CloningFailed)
            var currentProject = IJDDContext(clonedProject, project)
            for (stage in stages) {
                currentProject = stage.apply(currentProject, executor).bind()
            }
            currentProject.project
        }
    }
}
