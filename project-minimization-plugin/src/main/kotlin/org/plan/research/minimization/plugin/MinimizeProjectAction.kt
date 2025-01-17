package org.plan.research.minimization.plugin

import org.plan.research.minimization.plugin.services.MinimizationService
import org.plan.research.minimization.plugin.settings.ui.beforeExecutionDialog

import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.impl.ProjectViewPane
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbService

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * An action class responsible for minimizing a given project.
 */
class MinimizeProjectAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread =
        ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = true
        val project = e.project ?: run {
            e.presentation.isEnabled = false
            return
        }

        val isDumb = DumbService.isDumb(project)
        e.presentation.isEnabled = !isDumb
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val dialog = beforeExecutionDialog(project)
        if (!dialog.showAndGet()) {
            return
        }

        val minimizationService = project.service<MinimizationService>()
        minimizationService.minimizeProject { context ->
            withContext(Dispatchers.EDT) {
                val projectView = ProjectView.getInstance(project)
                projectView.changeView(ProjectViewPane.ID)
                projectView.select(null, context.projectDir, false)
            }
        }
    }
}
