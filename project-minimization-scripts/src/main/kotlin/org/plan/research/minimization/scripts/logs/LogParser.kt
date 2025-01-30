package org.plan.research.minimization.scripts.logs

import org.plan.research.minimization.plugin.benchmark.BenchmarkConfig
import org.plan.research.minimization.plugin.benchmark.BenchmarkProject
import org.plan.research.minimization.scripts.logs.model.LinesMetric
import org.plan.research.minimization.scripts.logs.model.ProjectStatistics
import org.plan.research.minimization.scripts.logs.model.StageStatistics
import org.plan.research.minimization.scripts.logs.model.ThroughMinimizationStatistics

import com.charleskorn.kaml.Yaml

import java.nio.file.Path
import java.time.LocalTime
import java.time.format.DateTimeFormatter

import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.readLines
import kotlin.io.path.readText
import kotlin.time.Duration
import kotlin.time.toKotlinDuration
import kotlinx.serialization.decodeFromString

class LogParser {
    fun parseLogs(baseDir: Path, stageNames: List<String>): List<ProjectStatistics> {
        val configPath = baseDir.resolve("config.yaml")
        val config = loadConfig(configPath)

        return config.projects.mapNotNull { project ->
            val logs = findLatestStatisticsLog(baseDir, project.path)
                ?.readLines()
                ?: return@mapNotNull null
            val relevantLogs = logs
                .extractRelevantLogsFor("Project minimization for project: ${project.name}")
                ?: return@mapNotNull null
            project.gatherStatistics(
                relevantLogs,
                stageNames,
            )
        }
    }

    private fun BenchmarkProject.gatherStatistics(
        relevantLogs: List<String>,
        stageNames: List<String>,
    ): ProjectStatistics {
        val projectLinesMetric = LinesMetric(
            totalLines = extractStat(relevantLogs, TOTAL_NAME),
            blankLines = extractStat(relevantLogs, BLANK_NAME),
            commentLines = extractStat(relevantLogs, COMMENT_NAME),
            codeLines = extractStat(relevantLogs, CODE_NAME),
        )

        return ProjectStatistics(
            projectName = name,
            elapsed = calculateDuration(relevantLogs.first(), relevantLogs.last()),
            ktFiles = extractStat(relevantLogs, "Kotlin files"),
            lines = projectLinesMetric,
            numberOfCompilations = relevantLogs.count { it.contains("Project dir:") },
            stageMetrics = buildList {
                add(StageStatistics(name = "<tmp>", linesMetric = projectLinesMetric.stale()))
                stageNames.forEach { stageName ->
                    add(
                        extractStageStatistics(
                            stageName,
                            relevantLogs.extractRelevantLogsFor("$stageName level stage") ?: return@forEach,
                            this.last(),
                        ),
                    )
                }
            }.drop(1),
        )
    }

    private fun loadConfig(configPath: Path): BenchmarkConfig {
        val configFile = configPath
            .takeIf { it.exists() } ?: throw IllegalArgumentException("Configuration file not found at: $configPath")
        return Yaml.Companion.default.decodeFromString(configFile.readText())
    }

    private fun findLatestStatisticsLog(baseDir: Path, projectPath: String): Path? {
        val logsDir = baseDir
            .resolve(projectPath)
            .resolve("minimization-logs")
            .takeIf { it.exists() && it.isDirectory() }
            ?.toFile()
            ?: return null

        val executionDirs = logsDir.listFiles { file -> file.isDirectory } ?: return null
        val latestExecutionDir = executionDirs.maxByOrNull { it.name } ?: return null
        val statisticsLog = latestExecutionDir.resolve("statistics.log")
        return statisticsLog.toPath().takeIf { it.exists() }
    }

    private fun calculateDuration(startLine: String, endLine: String): Duration {
        val startTime = extractTime(startLine)
        val endTime = extractTime(endLine)
        return java.time.Duration.between(startTime, endTime)
            .toKotlinDuration()
    }

    private fun extractStageStatistics(
        stageName: String,
        relevantLogs: List<String>,
        previousValue: StageStatistics,
    ): StageStatistics {
        val previousLines = previousValue.linesMetric
        return StageStatistics(
            name = stageName,
            linesMetric = LinesMetric(
                totalLines = previousLines.totalLines.continueWith(extractSingleMetric(relevantLogs, TOTAL_NAME)),
                blankLines = previousLines.blankLines.continueWith(extractSingleMetric(relevantLogs, BLANK_NAME)),
                commentLines = previousLines.commentLines.continueWith(extractSingleMetric(relevantLogs, COMMENT_NAME)),
                codeLines = previousLines.codeLines.continueWith(extractSingleMetric(relevantLogs, CODE_NAME)),
            ),
        )
    }

    private fun List<String>.extractRelevantLogsFor(name: String): List<String>? {
        val startLine = find { it.contains("Start $name") } ?: return null
        val endLine = find { it.contains("End $name") } ?: return null
        return subList(indexOf(startLine), indexOf(endLine) + 1)
    }

    private fun extractTime(logLine: String): LocalTime {
        val timePart = logLine.substringBefore(" - ")
        return LocalTime.parse(timePart, DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
    }

    private fun extractStat(logs: List<String>, statName: String): ThroughMinimizationStatistics {
        val statLines = logs.filter { it.contains(statName) }
        val before = statLines.firstOrNull().extractMetric(statName)
        val after = statLines.lastOrNull().extractMetric(statName)
        return ThroughMinimizationStatistics(before, after)
    }

    private fun extractSingleMetric(logs: List<String>, statName: String) = logs
        .lastOrNull { it.contains(statName) }
        .extractMetric(statName)

    private fun String?.extractMetric(metricName: String) = this
        ?.substringAfter("$metricName: ")
        ?.trim()
        ?.toIntOrNull()
        ?: 0

    companion object {
        private const val BLANK_NAME = "Blank LOC"
        private const val CODE_NAME = "Code LOC"
        private const val COMMENT_NAME = "Comment LOC"
        private const val TOTAL_NAME = "Total LOC"
    }
}
