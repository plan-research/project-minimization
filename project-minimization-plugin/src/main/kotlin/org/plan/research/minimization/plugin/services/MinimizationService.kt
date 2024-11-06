package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.errors.MinimizationError
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.settings.MinimizationPluginState

import arrow.core.raise.either
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportSequentialProgress
import mu.KotlinLogging

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

@Service(Service.Level.PROJECT)
class MinimizationService(project: Project, private val coroutineScope: CoroutineScope) {
    private val stages by project.service<MinimizationPluginState>()
        .stateObservable
        .stages
        .observe { it }
    private val executor = project.service<MinimizationStageExecutorService>()
    private val projectCloning = project.service<ProjectCloningService>()
    private val generalLogger = KotlinLogging.logger {}

    fun minimizeProject(project: Project) =
        coroutineScope.async {
            withBackgroundProgress(project, "Minimizing project") {
                either {
                    project.service<MinimizationPluginState>().freezeSettings(true)
                    generalLogger.info { "Start Project minimization" }

                    generalLogger.info { "Clonning project..." }
                    val clonedProject = projectCloning.clone(project)
                        ?: raise(MinimizationError.CloningFailed)
                    projectCloning.forceImportGradleProject(clonedProject)
                    generalLogger.info { "Project clone end" }

                    var currentProject = IJDDContext(clonedProject, project)

                    reportSequentialProgress(stages.size) { reporter ->
                        for (stage in stages) {
                            reporter.itemStep("Minimization step: ${stage.name}") {
                                currentProject = stage.apply(currentProject, executor).bind()
                            }
                            projectCloning.forceImportGradleProject(currentProject.project)
                        }
                    }

                    currentProject.project

                    project.service<MinimizationPluginState>().freezeSettings(false)
                }.onRight {
                    generalLogger.info { "End Project minimization" }
                }.onLeft { error ->
                    generalLogger.info { "End Project minimization" }
                    generalLogger.error { "End minimizeProject with error: $error" }
                }
            }
        }
}
