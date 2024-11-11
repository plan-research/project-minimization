package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.errors.MinimizationError
import org.plan.research.minimization.plugin.model.HeavyIJDDContext
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.LightIJDDContext
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
    private val logger = KotlinLogging.logger {}

    fun minimizeProject(project: Project, onComplete: suspend (IJDDContext) -> Unit = { }) =
        coroutineScope.async {
            withBackgroundProgress(project, "Minimizing project") {
                either {
                    logger.info { "Start Project minimization" }
                    var context: IJDDContext = HeavyIJDDContext(project)

                    logger.info { "Clonning project..." }
                    context = projectCloning.clone(context)
                        ?: raise(MinimizationError.CloningFailed)
                    importIfNeeded(context)
                    logger.info { "Project clone end" }

                    reportSequentialProgress(stages.size) { reporter ->
                        for (stage in stages) {
                            reporter.itemStep("Minimization step: ${stage.name}") {
                                context = stage.apply(context, executor).bind()
                            }
                            importIfNeeded(context)
                        }
                    }

                    context.also { onComplete(it) }
                }.onRight {
                    logger.info { "End Project minimization" }
                }.onLeft { error ->
                    logger.info { "End Project minimization" }
                    logger.error { "End minimizeProject with error: $error" }
                }
            }
        }

    private suspend fun importIfNeeded(context: IJDDContext) {
        if (context is HeavyIJDDContext) {
            projectCloning.forceImportGradleProject(context.project)
        }
    }
}
