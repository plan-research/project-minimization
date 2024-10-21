package org.plan.research.minimization.plugin.services

import arrow.core.raise.either
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportSequentialProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import mu.KotlinLogging
import org.plan.research.minimization.plugin.errors.MinimizationError
import org.plan.research.minimization.plugin.logging.Loggers
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings

@Service(Service.Level.PROJECT)
class MinimizationService(project: Project, private val coroutineScope: CoroutineScope) {
    private val stages = project.service<MinimizationPluginSettings>().state.stages
    private val executor = project.service<MinimizationStageExecutorService>()
    private val projectCloning = project.service<ProjectCloningService>()

    fun minimizeProject(project: Project) =
        coroutineScope.async {
            withBackgroundProgress(project, "Minimizing project") {
                either {
                    Loggers.generalLogger.info { "Start Project minimization" }

                    Loggers.generalLogger.info { "Clonning project..." }
                    val clonedProject = projectCloning.clone(project)
                        ?: raise(MinimizationError.CloningFailed)
                    Loggers.generalLogger.info { "Project clone end" }
                        
                    var currentProject = IJDDContext(clonedProject, project)

                    reportSequentialProgress(stages.size) { reporter ->
                        for (stage in stages) {
                            reporter.itemStep("Minimization step: ${stage.name}") {
                                currentProject = stage.apply(currentProject, executor).bind()
                            }
                        }
                    }

                    currentProject.project
                }.onRight {
                    Loggers.generalLogger.info { "End Project minimization" }
                }.onLeft { error ->
                    Loggers.generalLogger.info { "End Project minimization" }
                    Loggers.generalLogger.error { "End minimizeProject with error: $error" }
                }
            }
        }
}
