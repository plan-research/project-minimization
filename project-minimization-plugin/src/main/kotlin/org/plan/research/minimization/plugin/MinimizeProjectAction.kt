package org.plan.research.minimization.plugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.plan.research.minimization.plugin.services.MinimizationService

/**
 * An action class responsible for minimizing a given project.
 */
class MinimizeProjectAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        MinimizationTask(project).queue()
    }

    private class MinimizationTask(project: Project) : Backgroundable(project, "Minimizing Project") {
        val minimizationService = project.service<MinimizationService>()

        override fun run(indicator: ProgressIndicator) {
            indicator.start()
            val job = minimizationService.minimizeProject(project)
            CoroutineScope(Dispatchers.Default).launch {
                job.await()
                indicator.stop()
            }
        }
    }
}