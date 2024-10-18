package org.plan.research.minimization.plugin

import org.plan.research.minimization.plugin.services.BenchmarkingService

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.toNioPathOrNull

import kotlin.io.path.relativeTo

class BenchmarkAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        if (!project.name.endsWith("dataset")) {
            return
        }
        val benchmarkingService = project.service<BenchmarkingService>()
        benchmarkingService.benchmark(
            onFailure = {
                println("Failed to benchmark project '${project.name}'. Error: ${it ?: "Failed to get config"}")
            },
            onSuccess = {
                println(
                    "Successfully benchmarked project '${project.name}'. The minimized project is stored in ${
                        it.guessProjectDir()?.toNioPathOrNull()?.relativeTo(project.guessProjectDir()!!.toNioPath())
                    }",
                )
            },
        )
    }
}
