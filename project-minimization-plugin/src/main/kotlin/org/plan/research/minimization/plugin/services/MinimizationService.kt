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
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings

@Service(Service.Level.PROJECT)
class MinimizationService(project: Project, private val coroutineScope: CoroutineScope) {
    private val stages = project.service<MinimizationPluginSettings>().state.stages
    private val executor = project.service<MinimizationStageExecutorService>()
    private val projectCloning = project.service<ProjectCloningService>()

    private val workingLogger = KotlinLogging.logger("WORKING")
    private val logger = KotlinLogging.logger {}

    fun minimizeProject(project: Project) =
        coroutineScope.async {
            withBackgroundProgress(project, "Minimizing project") {
                either {
                    workingLogger.info { "Start Project minimization" }

                    workingLogger.info { "Clonning project..." }
                    val clonedProject = projectCloning.clone(project)
                        ?: raise(MinimizationError.CloningFailed)
                    workingLogger.info { "Project clone end" }      
                        
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
                    workingLogger.info { "End Project minimization" }
                }.onLeft { error ->
                    workingLogger.info { "End Project minimization" }
                    logger.error { "End minimizeProject with error: $error" }
                }
            }
        }
}
