package org.plan.research.minimization.plugin.services

import arrow.core.raise.either
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.plan.research.minimization.plugin.model.ProjectDDVersion
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings

@Service(Service.Level.PROJECT)
class MinimizationService(project: Project) {
    private val stages = project.service<MinimizationPluginSettings>().state.stages
    private val executor = project.service<MinimizationStageExecutorService>()

    suspend fun minimizeProject(project: Project) = either {
        var currentProject = ProjectDDVersion(project)
        for (stage in stages) {
            currentProject = stage.apply(currentProject, executor).bind()
        }
        currentProject.project
    }
}
