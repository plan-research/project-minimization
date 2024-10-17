package org.plan.research.minimization.plugin

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbService
import org.plan.research.minimization.plugin.services.MinimizationService

/**
 * An action class responsible for minimizing a given project.
 */
class MinimizeProjectAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread =
        ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isVisible = true
        if (project == null) {
            e.presentation.isEnabled = false
            return
        }

        val isDumb = DumbService.isDumb(project)
        e.presentation.isEnabled = !isDumb
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val minimizationService = project.service<MinimizationService>()
        minimizationService.minimizeProject(project)
    }
}