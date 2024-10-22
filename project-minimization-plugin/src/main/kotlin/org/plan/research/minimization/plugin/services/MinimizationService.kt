package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.errors.MinimizationError
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings

import arrow.core.raise.either
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportSequentialProgress
import kotlinx.coroutines.*

@Service(Service.Level.PROJECT)
class MinimizationService(project: Project, private val coroutineScope: CoroutineScope) {
    private val stages = project.service<MinimizationPluginSettings>().state.stages
    private val executor = project.service<MinimizationStageExecutorService>()
    private val projectCloning = project.service<ProjectCloningService>()

    fun minimizeProject(project: Project) =
        coroutineScope.async {
            withBackgroundProgress(project, "Minimizing project") {
                either {
                    val clonedProject = projectCloning.clone(project)
                        ?: raise(MinimizationError.CloningFailed)
                    var currentProject = IJDDContext(clonedProject, project)
                    try {
                        reportSequentialProgress(stages.size) { reporter ->
                            for (stage in stages) {
                                reporter.itemStep("Minimization step: ${stage.name}") {
                                    currentProject = stage.apply(currentProject, executor).bind()
                                }
                            }
                        }

                        currentProject.project
                    } catch (e: Throwable) {
                        // withContext(Dispatchers.EDT + NonCancellable) {
                        // ProjectManager.getInstance().closeAndDispose(currentProject.project)
                        // }
                        throw e
                    }
                }
            }
        }
}
