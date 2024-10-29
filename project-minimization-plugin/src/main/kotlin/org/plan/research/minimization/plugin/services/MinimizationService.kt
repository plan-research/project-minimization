package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.errors.MinimizationError
import org.plan.research.minimization.plugin.model.HeavyIJDDContext
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings

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
    private val stages = project.service<MinimizationPluginSettings>().state.stages
    private val executor = project.service<MinimizationStageExecutorService>()
    private val projectCloning = project.service<ProjectCloningService>()
    private val generalLogger = KotlinLogging.logger {}

    fun minimizeProject(project: Project) =
        coroutineScope.async {
            withBackgroundProgress(project, "Minimizing project") {
                either {
                    generalLogger.info { "Start Project minimization" }
                    var currentProject = HeavyIJDDContext(project)

                    generalLogger.info { "Clonning project..." }
                    currentProject = projectCloning.clone(currentProject)
                        ?: raise(MinimizationError.CloningFailed)
                    projectCloning.forceImportGradleProject(currentProject.project)
                    generalLogger.info { "Project clone end" }

                    reportSequentialProgress(stages.size) { reporter ->
                        for (stage in stages) {
                            reporter.itemStep("Minimization step: ${stage.name}") {
                                currentProject = stage.apply(currentProject, executor).bind() as HeavyIJDDContext
                            }
                            projectCloning.forceImportGradleProject(currentProject.project)
                        }
                    }

                    currentProject.project
                }.onRight {
                    generalLogger.info { "End Project minimization" }
                }.onLeft { error ->
                    generalLogger.info { "End Project minimization" }
                    generalLogger.error { "End minimizeProject with error: $error" }
                }
            }
        }
}
