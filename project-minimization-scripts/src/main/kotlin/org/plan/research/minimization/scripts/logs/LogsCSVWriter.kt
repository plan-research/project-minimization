package org.plan.research.minimization.scripts.logs

import org.plan.research.minimization.plugin.model.benchmark.logs.LinesMetric
import org.plan.research.minimization.plugin.model.benchmark.logs.ProjectStatistics

import com.github.doyaaaaaken.kotlincsv.client.ICsvFileWriter
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter

import java.nio.file.Path

import kotlin.io.path.outputStream
import kotlin.time.Duration

class LogsCSVWriter(val logs: List<ProjectStatistics>) {
    init {
        require(logs.map { it.stageMetrics.map { it.name } }.distinct().size == 1) {
            "Different project has different stages. Dumping to CSV is impossible."
        }
    }

    fun writeToCsv(path: Path) = csvWriter().open(path.outputStream()) {
        writeHeader()
        logs.forEach { writeProject(it) }
    }

    private fun ICsvFileWriter.writeHeader() {
        val stagesName = logs.first().stageMetrics.map { "After ${it.name}" }
        val locColumns = (listOf("Initial") + stagesName).flatMap { name ->
            listOf(
                "Total",
                "Blank",
                "Comment",
                "Code",
            ).map { "${name.capitalize()} LOC ($it)" }
        }.toTypedArray()
        writeRow(
            "Project Name",
            "Elapsed Time",
            "Initial Files Number",
            "Minimized Files Number",
            *locColumns,
            "Number of Compilations",
        )
    }

    private fun ICsvFileWriter.writeProject(project: ProjectStatistics) {
        writeRow(
            project.projectName,
            project.elapsed.dump(),
            project.ktFiles.before,
            project.ktFiles.after,
            *project.lines.flattenBefore(),
            *(project.stageMetrics.flatMap { it.linesMetric.flatten() }.toTypedArray()),
            project.numberOfCompilations,
        )
    }

    private fun LinesMetric.flatten() = listOf(totalLines.after, blankLines.after, commentLines.after, codeLines.after)
    private fun String.capitalize() = replaceFirstChar { it.uppercase() }
    private fun Duration.dump(): String {
        val (minutes, seconds) = this.toComponents { minutes, seconds, _ -> minutes to seconds }
        return if (minutes > 0) {
            "$minutes m $seconds s"
        } else {
            "$seconds s"
        }
    }

    private fun LinesMetric.flattenBefore() =
        arrayOf(totalLines.before, blankLines.before, commentLines.before, codeLines.before)
}
