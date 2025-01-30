package org.plan.research.minimization.plugin.headless

import org.plan.research.minimization.plugin.services.MinimizationResult
import org.plan.research.minimization.plugin.services.MinimizationService
import org.plan.research.minimization.plugin.services.ProjectOpeningService

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.danger
import com.github.ajalt.mordant.terminal.success
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.ex.ProjectManagerEx

import java.nio.file.Path

class MinimizeProjectCliCommand : SuspendingCliktCommand() {
    val projectPath: Path by option(
        "-p",
        "--project-path",
        help = "Path to the project to minimize",
    ).path(mustExist = true, canBeFile = false, canBeDir = true).required()
    lateinit var terminal: Terminal

    override suspend fun run() {
        terminal = Terminal()
        val project = service<ProjectOpeningService>().openProject(projectPath) ?: run {
            reportOpeningProblem()
            return
        }
        val minimizationResult = project
            .service<MinimizationService>()
            .minimizeProjectAsync()
        reportMinimizationResult(minimizationResult)
        ApplicationManager.getApplication().exit(false, true, false)
    }
    private fun reportOpeningProblem() {
        terminal.danger("""
            Error with opening project with path $projectPath.
            There is some possible reasons:
                - Project folder does not contain any project
                - Project folder does not contain *gradle* project
                - Project is corrupted.
            You can try to open the project using IDEA and run the minimization again manually. 
            """.trimIndent())
    }
    private suspend fun reportMinimizationResult(minimizationResult: MinimizationResult) = minimizationResult
        .onLeft {
            // TODO: explain errors
            terminal.danger("""
                Minimization has failed. The reason is $it. Please submit the logs to the developers or retry using IDEA.
            """.trimIndent())
        }
        .onRight {
            terminal.success("""
                The project has been successfully minimized. 
                The result of the minimization is in folder ${TextStyles.hyperlink(it.projectDir.url)}.
            """.trimIndent())
            // TODO: Pretty-print statistics
            ProjectManagerEx.getInstanceEx().forceCloseProjectAsync(it.project)
        }
}
