import org.plan.research.minimization.plugin.benchmark.BenchmarkConfig

import com.charleskorn.kaml.Yaml

import java.io.File
import java.nio.file.Path
import java.time.LocalTime
import java.time.format.DateTimeFormatter

import kotlinx.serialization.decodeFromString

// Data class to hold parsed project statistics
data class ProjectStatistics(
    val projectName: String,
    val durationMinutes: Long,
    val ktFilesBefore: Int,
    val ktFilesAfter: Int,

    val totalLocBefore: Int,
    val totalLocStages: List<Int>,
    val totalLocAfter: Int,

    val blankLocBefore: Int,
    val blankLocStages: List<Int>,
    val blankLocAfter: Int,

    val commentLocBefore: Int,
    val commentLocStages: List<Int>,
    val commentLocAfter: Int,

    val codeLocBefore: Int,
    val codeLocStages: List<Int>,
    val codeLocAfter: Int,

    val numberOfCompilations: Int,
)

data class StageLoc(
    val totalLocAfter: Int,
    val blankLocAfter: Int,
    val commentLocAfter: Int,
    val codeLocAfter: Int,
)

class LogParser {
    @Suppress("TOO_LONG_FUNCTION")
    fun parseLogs(baseDir: Path, stageNames: List<String>, outputCsvPath: Path) {
        val configPath = baseDir.resolve("config.yaml")
        val config = loadConfig(configPath)

        val projectsStatistics = mutableListOf<ProjectStatistics>()

        config.projects.forEach { project ->
            val logFilePath = findLatestStatisticsLog(baseDir, project.path)

            logFilePath ?: run {
                return@forEach
            }

            val logs = logFilePath.toFile().readLines()

            val startLine = logs.find { it.contains("Start Project minimization for project: ${project.name}") }
            val endLine = logs.find { it.contains("End Project minimization for project: ${project.name}") }

            if (startLine != null && endLine != null) {
                val relevantLogs = logs.subList(logs.indexOf(startLine), logs.indexOf(endLine) + 1)

                val durationMinutes = calculateDuration(startLine, endLine)

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

                val totalLocStages: MutableList<Int> = mutableListOf()
                val blankLocStages: MutableList<Int> = mutableListOf()
                val commentLocStages: MutableList<Int> = mutableListOf()
                val codeLocStages: MutableList<Int> = mutableListOf()

                stageNames.forEach { stageName ->
                    val stageResults = extractStageStatistics(relevantLogs, stageName)
                    totalLocStages.add(stageResults.totalLocAfter)
                    blankLocStages.add(stageResults.blankLocAfter)
                    commentLocStages.add(stageResults.commentLocAfter)
                    codeLocStages.add(stageResults.codeLocAfter)
                }

                val numberOfCompilations = relevantLogs.count { it.contains("Project dir:") }

                projectsStatistics.add(
                    ProjectStatistics(
                        project.name,
                        durationMinutes,
                        ktFilesBefore,
                        ktFilesAfter,
                        totalLocBefore,
                        totalLocStages,
                        totalLocAfter,
                        blankLocBefore,
                        blankLocStages,
                        blankLocAfter,
                        commentLocBefore,
                        commentLocStages,
                        commentLocAfter,
                        codeLocBefore,
                        codeLocStages,
                        codeLocAfter,
                        numberOfCompilations,
                    ),
                )
            }
        }

        writeCsv(projectsStatistics, stageNames, outputCsvPath)
    }

    private fun loadConfig(configPath: Path): BenchmarkConfig {
        val configFile = configPath.toFile()

        if (!configFile.exists()) {
            throw IllegalArgumentException("Configuration file not found at: $configPath")
        }

        val content = configFile.readText()

        return Yaml.default.decodeFromString(content)
    }

    private fun findLatestStatisticsLog(baseDir: Path, projectPath: String): Path? {
        val logsDir = baseDir.resolve(projectPath).resolve("minimization-logs").toFile()

        if (!logsDir.exists() || !logsDir.isDirectory) {
            return null
        }

        val executionDirs = logsDir.listFiles { file -> file.isDirectory } ?: return null

        val latestExecutionDir = executionDirs.maxByOrNull { it.name } ?: return null

        val statisticsLog = latestExecutionDir.resolve("statistics.log")
        return if (statisticsLog.exists()) statisticsLog.toPath() else null
    }

    private fun calculateDuration(startLine: String, endLine: String): Long {
        val startTime = extractTime(startLine)
        val endTime = extractTime(endLine)
        return java.time.Duration.between(startTime, endTime)
            .toMinutes()
    }

    private fun extractStageStatistics(logs: List<String>, stageName: String): StageLoc {
        val startStageLine = logs.find { it.contains("Start $stageName level stage") }
        val endStageLine = logs.find { it.contains("End $stageName level stage") }

        if (startStageLine != null && endStageLine != null) {
            val stageLogs = logs.subList(logs.indexOf(startStageLine), logs.indexOf(endStageLine) + 1)

            val totalLocAfter = extractStat(stageLogs, "Total LOC", first = false)

            val blankLocAfter = extractStat(stageLogs, "Blank LOC", first = false)

            val commentLocAfter = extractStat(stageLogs, "Comment LOC", first = false)

            val codeLocAfter = extractStat(stageLogs, "Code LOC", first = false)

            return StageLoc(
                totalLocAfter,
                blankLocAfter,
                commentLocAfter,
                codeLocAfter,
            )
        }
        return StageLoc(0, 0, 0, 0)
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

    @Suppress("TOO_LONG_FUNCTION")
    private fun writeCsv(projectsStatistics: List<ProjectStatistics>, stageNames: List<String>, outputCsvPath: Path) {
        val file = File(outputCsvPath.toUri())
        file.bufferedWriter().use { writer ->
            val header = buildString {
                append("Project Name,Duration (minutes),Kotlin Files Before,Kotlin Files After,Total LOC Before,")
                stageNames.forEach { stageName ->
                    append("$stageName Total LOC,")
                }
                append("Total LOC After,Blank LOC Before,")
                stageNames.forEach { stageName ->
                    append("$stageName Blank LOC,")
                }
                append("Blank LOC After,Comment LOC Before,")
                stageNames.forEach { stageName ->
                    append("$stageName Comment LOC,")
                }
                append("Comment LOC After,Code LOC Before,")
                stageNames.forEach { stageName ->
                    append("$stageName Code LOC,")
                }
                append("Code LOC After,Number of Compilations\n")
            }

            writer.write(header)

            projectsStatistics.forEach { stats ->
                val row = buildString {
                    append("${stats.projectName},${stats.durationMinutes},${stats.ktFilesBefore},${stats.ktFilesAfter},")
                    append("${stats.totalLocBefore},")
                    stats.totalLocStages.forEach { append("$it,") }
                    append("${stats.totalLocAfter},${stats.blankLocBefore},")
                    stats.blankLocStages.forEach { append("$it,") }
                    append("${stats.blankLocAfter},${stats.commentLocBefore},")
                    stats.commentLocStages.forEach { append("$it,") }
                    append("${stats.commentLocAfter},${stats.codeLocBefore},")
                    stats.codeLocStages.forEach { append("$it,") }
                    append("${stats.codeLocAfter},${stats.numberOfCompilations}\n")
                }
                writer.write(row)
            }
        }
    }
}

// Usage: <Benchmark-Project-Dir> <outputCsvPath> <stageName1> [<stageName2> ...]
fun main(args: Array<String>) {
    if (args.size < 2) {
        return
    }

    val baseDir = Path.of(args[0])
    val outputCsvPath = Path.of(args[1])
    val stageNames = args.slice(2 until args.size)

    LogParser().parseLogs(baseDir, stageNames, outputCsvPath)
}
