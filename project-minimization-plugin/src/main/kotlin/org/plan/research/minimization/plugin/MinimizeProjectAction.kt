package org.plan.research.minimization.plugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import org.plan.research.minimization.plugin.services.MinimizationService

/**
 * An action class responsible for minimizing a given project.
 */
class MinimizeProjectAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val minimizationService = project.service<MinimizationService>()
        minimizationService.minimizeProject(project)
    }
}