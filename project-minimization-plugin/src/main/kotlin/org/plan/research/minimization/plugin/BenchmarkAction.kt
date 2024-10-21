package org.plan.research.minimization.plugin

import org.plan.research.minimization.plugin.benchmark.BenchmarkProject
import org.plan.research.minimization.plugin.benchmark.BenchmarkResultSubscriber
import org.plan.research.minimization.plugin.errors.MinimizationError
import org.plan.research.minimization.plugin.services.BenchmarkingService

import com.intellij.notification.*
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir

class BenchmarkAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        // TODO: make default choices for terminating and new projects
        val benchmarkingService = project.service<BenchmarkingService>()
        benchmarkingService.benchmark(BenchmarkResultNotifier(project))
    }

    private class BenchmarkResultNotifier(private val root: Project) : BenchmarkResultSubscriber {
        private val notificationGroup = NotificationGroupManager
            .getInstance()
            .getNotificationGroup("org.plan.research.minimization.benchmark.result")

        override fun onSuccess(project: Project, config: BenchmarkProject) {
            val link = """<a href="file://${project.guessProjectDir()?.path}">${project.name}</a>"""
            notificationGroup
                .createNotification(
                    title = "Minimization successful for ${config.name}",
                    content = "Minimization has been successfully completed. The minimized project is stored in $link",
                    type = NotificationType.INFORMATION,
                )
                .setListener(NotificationListener.URL_OPENING_LISTENER)
                .setImportant(false)
                .notify(root)
        }

        override fun onFailure(error: MinimizationError, config: BenchmarkProject) {
            notificationGroup
                .createNotification(
                    title = "Minimization failed for ${config.name}",
                    content = "Minimization has internally failed with '$error' Error. Please, check the logs for more details.",
                    type = NotificationType.WARNING,
                )
                .setListener(NotificationListener.URL_OPENING_LISTENER)
                .setImportant(false)
                .notify(root)
        }

        override fun onConfigCreationError() {
            notificationGroup
                .createNotification(
                    title = "Config creation error. Aborted",
                    content = "The minimization plugin could not find a valid dataset. Please, check the logs for more details.",
                    type = NotificationType.ERROR,
                )
                .setListener(NotificationListener.URL_OPENING_LISTENER)
                .setImportant(false)
                .notify(root)
        }
    }
}
