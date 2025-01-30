package org.plan.research.minimization.plugin.headless

import org.plan.research.minimization.plugin.services.BenchmarkService
import org.plan.research.minimization.plugin.services.ProjectOpeningService

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.danger
import com.github.ajalt.mordant.terminal.success
import com.intellij.openapi.components.service

import java.nio.file.Path

class RunBenchmarkCliCommand : SuspendingCliktCommand() {
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
        // TODO: rewrite benchmark to support result values
        project
            .service<BenchmarkService>()
            .asyncBenchmark()
    }

    private fun reportOpeningProblem() {
        terminal.danger("""
            Error with opening benchmark with path $projectPath.
            There is some possible reasons:
                - Benchmark folder does not contain any project
                - Benchmark is corrupted.
            You can try to open the benchmark using IDEA and run the benchmarking again manually. 
            """.trimIndent())
    }
    private fun procesResults() {
        terminal.success("Benchmarking is done successfully")
        // TODO?: print pretty table with benchmarking results
    }
}
