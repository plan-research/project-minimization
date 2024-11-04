package org.plan.research.minimization.plugin

import org.plan.research.minimization.plugin.services.MinimizationService
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbService

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
        project.service<MinimizationPluginSettings>().state.updateSettingsState()
        val minimizationService = project.service<MinimizationService>()
        minimizationService.minimizeProject(project)
    }
}
