package org.plan.research.minimization.plugin

import org.plan.research.minimization.plugin.services.BenchmarkService

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbService

class BenchmarkAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

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
        val benchmarkingService = project.service<BenchmarkService>()
        benchmarkingService.benchmark()
    }
}
