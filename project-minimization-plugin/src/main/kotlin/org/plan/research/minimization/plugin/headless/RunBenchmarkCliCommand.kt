package org.plan.research.minimization.plugin.headless

import org.plan.research.minimization.plugin.benchmark.BenchmarkSettings
import org.plan.research.minimization.plugin.model.benchmark.logs.ProjectStatistics
import org.plan.research.minimization.plugin.model.benchmark.logs.StageStatistics
import org.plan.research.minimization.plugin.services.BenchmarkService
import org.plan.research.minimization.plugin.services.ProjectOpeningService

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.rendering.OverflowWrap
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextColors.brightGreen
import com.github.ajalt.mordant.rendering.TextColors.brightRed
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.dim
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.SectionBuilder
import com.github.ajalt.mordant.table.TableBuilder
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.danger
import com.github.ajalt.mordant.terminal.success
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

import java.nio.file.Path

import kotlin.time.Duration

class RunBenchmarkCliCommand : SuspendingCliktCommand() {
    val projectPath: Path by option(
        "-p",
        "--project-path",
        help = "Path to the project to minimize",
    ).path(mustExist = true, canBeFile = false, canBeDir = true).required()
    lateinit var terminal: Terminal
    override suspend fun run() {
        terminal = Terminal()
        terminal.tabWidth
        val project = service<ProjectOpeningService>().openProject(projectPath) ?: run {
            reportOpeningProblem()
            return
        }
        BenchmarkSettings.isBenchmarkingEnabled = true
        // TODO: rewrite benchmark to support result values
        project
            .service<BenchmarkService>()
            .asyncBenchmark()
        BenchmarkSettings.isBenchmarkingEnabled = false
        procesResults()
        printLogs(project)
        ApplicationManager.getApplication().exit(false, true, false)
    }

    private fun reportOpeningProblem() {
        terminal.danger(
            """
            Error with opening benchmark with path $projectPath.
            There is some possible reasons:
                - Benchmark folder does not contain any project
                - Benchmark is corrupted.
            You can try to open the benchmark using IDEA and run the benchmarking again manually. 
            """.trimIndent(),
        )
    }

    private fun procesResults() {
        terminal.success("Benchmarking is done successfully")
    }

    private fun printLogs(project: Project) {
        val logs = project.service<BenchmarkService>().parseBenchmarkLogs()
        for (statistics in logs) {
            terminal.println(statistics.toTable())
        }
    }

    private fun ProjectStatistics.toTable() = table {
        captionTop(projectName)
        buildHeader()
        body {
            style = brightGreen
            align = TextAlign.CENTER
            column(0) {
                align = TextAlign.LEFT
                cellBorders = Borders.ALL
                style = TextColors.brightWhite + bold
            }
            rowStyles(TextStyle(), dim.style)
            cellBorders = Borders.TOP_BOTTOM
            row(
                "Before Minimization",
                ktFiles.before,
                lines.totalLines.before,
                lines.blankLines.before,
                lines.commentLines.before,
                lines.codeLines.before,
                "",
                "",
            )
            stageMetrics.dropLast(1).forEach { rowForStage(it, ktFiles = ktFiles.before) }
            stageMetrics.lastOrNull()?.let {
                rowForStage(
                    it,
                    ktFiles = ktFiles.after,
                    elapsedTime = elapsed.dump(),
                    compilations = numberOfCompilations,
                )
            }
        }
    }

    private fun SectionBuilder.rowForStage(
        stage: StageStatistics,
        // FIXME
        ktFiles: Any = "",
        // FIXME
        elapsedTime: Any = "",
        // FIXME
        compilations: Any = "",
    ) = stage.let { (name, lines) ->
        row(
            name,
            ktFiles,
            lines.totalLines.after,
            lines.blankLines.after,
            lines.commentLines.after,
            lines.codeLines.after,
            elapsedTime,
            compilations,
        )
    }

    private fun TableBuilder.buildHeader() = header {
        style = brightRed + bold
        @Suppress("MAGIC_NUMBER")
        column(0) {
            cellBorders = Borders.TOP_RIGHT_BOTTOM
        }
        @Suppress("MAGIC_NUMBER")
        column(7) {
            cellBorders = Borders.LEFT_TOP_BOTTOM
        }
        row {
            cellBorders = Borders.TOP_BOTTOM
            cell("")
            cell("")
            cell("LoC") {
                columnSpan = 4
                align = TextAlign.CENTER
            }
            cells("", "")
        }
        row(
            "Stage",
            ".kt Files",
            "Total",
            "Blank",
            "Comment",
            "Code",
            "Elapsed time",
            "Compilations",
        ) {
            cellBorders = Borders.ALL
            overflowWrap = OverflowWrap.NORMAL
        }
    }

    private fun Duration.dump(): String {
        val (minutes, seconds) = this.toComponents { minutes, seconds, _ -> minutes to seconds }
        return if (minutes > 0) {
            "$minutes m $seconds s"
        } else {
            "$seconds s"
        }
    }
}
