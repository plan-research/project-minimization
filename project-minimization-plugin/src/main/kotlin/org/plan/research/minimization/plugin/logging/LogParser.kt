package org.plan.research.minimization.plugin.logging

import java.io.File
import java.nio.file.Path
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// Data class to hold parsed project statistics
data class ProjectStatistics(
    val projectName: String,
    val durationMinutes: Long,
    val ktFilesBefore: Int,
    val ktFilesAfter: Int,
    val totalLocBefore: Int,
    val totalLocAfter: Int,
    val blankLocBefore: Int,
    val blankLocAfter: Int,
    val commentLocBefore: Int,
    val commentLocAfter: Int,
    val codeLocBefore: Int,
    val codeLocAfter: Int,
)

class LogParser {
    @Suppress("TOO_LONG_FUNCTION")
    fun parseLogs(logFilePath: Path, projectNames: List<String>, outputCsvPath: Path) {
        val logs = File(logFilePath.toUri()).readLines()
        val projectsStatistics = mutableListOf<ProjectStatistics>()

        projectNames.forEach { projectName ->
            val startLine = logs.find { it.contains("Start Project minimization for project: $projectName") }
            val endLine = logs.find { it.contains("End Project minimization for project: $projectName") }

            if (startLine != null && endLine != null) {
                val startTime = extractTime(startLine)
                val endTime = extractTime(endLine)
                val durationMinutes = java.time.Duration.between(startTime, endTime).toMinutes()

                val relevantLogs = logs.subList(logs.indexOf(startLine), logs.indexOf(endLine) + 1)

                val ktFilesBefore = extractStat(relevantLogs, "Kotlin files", first = true)
                val ktFilesAfter = extractStat(relevantLogs, "Kotlin files", first = false)

                val totalLocBefore = extractStat(relevantLogs, "Total LOC", first = true)
                val totalLocAfter = extractStat(relevantLogs, "Total LOC", first = false)

                val blankLocBefore = extractStat(relevantLogs, "Blank LOC", first = true)
                val blankLocAfter = extractStat(relevantLogs, "Blank LOC", first = false)

                val commentLocBefore = extractStat(relevantLogs, "Comment LOC", first = true)
                val commentLocAfter = extractStat(relevantLogs, "Comment LOC", first = false)

                val codeLocBefore = extractStat(relevantLogs, "Code LOC", first = true)
                val codeLocAfter = extractStat(relevantLogs, "Code LOC", first = false)

                projectsStatistics.add(
                    ProjectStatistics(
                        projectName,
                        durationMinutes,
                        ktFilesBefore,
                        ktFilesAfter,
                        totalLocBefore,
                        totalLocAfter,
                        blankLocBefore,
                        blankLocAfter,
                        commentLocBefore,
                        commentLocAfter,
                        codeLocBefore,
                        codeLocAfter,
                    ),
                )
            }
        }

        writeCsv(projectsStatistics, outputCsvPath)
    }

    private fun extractTime(logLine: String): LocalTime {
        val timePart = logLine.substringBefore(" - ")
        return LocalTime.parse(timePart, DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
    }

    private fun extractStat(logs: List<String>, statName: String, first: Boolean): Int {
        val statLines = logs.filter { it.contains(statName) }
        val targetLine = if (first) statLines.firstOrNull() else statLines.lastOrNull()
        return targetLine?.substringAfter("$statName: ")?.trim()?.toIntOrNull() ?: 0
    }

    private fun writeCsv(projectsStatistics: List<ProjectStatistics>, outputCsvPath: Path) {
        val file = File(outputCsvPath.toUri())
        file.bufferedWriter().use { writer ->
            writer.write(
                "Project Name,Duration (minutes),Kotlin Files Before,Kotlin Files After,Total LOC Before,Total LOC After," +
                    "Blank LOC Before,Blank LOC After,Comment LOC Before,Comment LOC After,Code LOC Before,Code LOC After\n",
            )

            projectsStatistics.forEach { stats ->
                writer.write(
                    "${stats.projectName},${stats.durationMinutes},${stats.ktFilesBefore},${stats.ktFilesAfter}," +
                        "${stats.totalLocBefore},${stats.totalLocAfter},${stats.blankLocBefore},${stats.blankLocAfter}," +
                        "${stats.commentLocBefore},${stats.commentLocAfter},${stats.codeLocBefore},${stats.codeLocAfter}\n",
                )
            }
        }
    }
}

// Usage: <logFilePath> <outputCsvPath> <projectName1> [<projectName2> ...]
fun main(args: Array<String>) {
    if (args.size < 3) {
        return
    }

    val logFilePath = Path.of(args[0])
    val outputCsvPath = Path.of(args[1])
    val projectNames = args.slice(2 until args.size)

    LogParser().parseLogs(logFilePath, projectNames, outputCsvPath)
}
