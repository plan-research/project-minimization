package org.plan.research.minimization.plugin.services

import arrow.core.raise.either
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings

@Service(Service.Level.PROJECT)
class MinimizationService(project: Project, private val coroutineScope: CoroutineScope) {
    private val stages = project.service<MinimizationPluginSettings>().state.stages
    private val executor = project.service<MinimizationStageExecutorService>()
    private val projectCloning = project.service<ProjectCloningService>()

     fun minimizeProject(project: Project) = coroutineScope.launch {
         val clonedProject = projectCloning.clone(project)
         if (clonedProject == null) {
             println("Failed to clone project")
             return@launch
         }
         val result = either {
             var currentProject = IJDDContext(clonedProject)
             for (stage in stages) {
                 currentProject = stage.apply(currentProject, executor).bind()
             }
             currentProject.project
         }
         result.fold(
             ifLeft = { println("MinimizationError = $it") },
             ifRight = { println("Minimization successful. Minimized project is saved at $it.") }
         )
     }
}
