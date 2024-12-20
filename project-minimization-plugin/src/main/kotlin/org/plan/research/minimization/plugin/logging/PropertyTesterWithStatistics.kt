package org.plan.research.minimization.plugin.logging

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.services.RootsManagerService

import com.intellij.openapi.components.service

import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Path

data class RootStatistics(
    var loc: Int = 0,
    var blankLoc: Int = 0,
    var commentLoc: Int = 0,
    var codeLoc: Int = 0,
    var ktFiles: Int = 0,
)

class PropertyTesterWithStatistics<C : IJDDContext, T : DDItem>(
    private val innerTester: PropertyTester<C, T>,
) : PropertyTester<C, T> {
    override suspend fun test(context: C, items: List<T>): PropertyTestResult<C> {
        val result = innerTester.test(context, items)
        result.fold({ _ -> },
            { value ->
                if (value != context) {
                    analyzeProject(context)
                    analyzeProject(value)
                }
            },
        )
        return result
    }

    private fun analyzeProject(context: C) {
        val roots = service<RootsManagerService>().findPossibleRoots(context)
        var projectStat = RootStatistics()
        val projectDirPath = context.projectDir.toNioPath()
        for (root in roots) {
            val fullPath = projectDirPath.resolve(root)
            val stat = statisticsForPath(fullPath)
            projectStat += stat
        }
        statLogger.info { "Project dir: ${context.projectDir}" }
        statLogger.info { "Total LOC: ${projectStat.loc}" }
        statLogger.info { "Blank LOC: ${projectStat.blankLoc}" }
        statLogger.info { "Comment LOC: ${projectStat.commentLoc}" }
        statLogger.info { "Code LOC: ${projectStat.codeLoc}" }
        statLogger.info { "Kotlin files: ${projectStat.ktFiles}" }
    }

    private fun statisticsForPath(root: Path): RootStatistics {
        val clocOutput = runCloc(root.toString())
        return parseClocOutput(clocOutput)
    }

    private fun runCloc(path: String): String {
        val processBuilder = ProcessBuilder("cloc", path)
        processBuilder.redirectErrorStream(true)
        val process = processBuilder.start()
        val result = StringBuilder()
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                result.append(line).append("\n")
            }
        }
        process.waitFor()
        return result.toString()
    }

    private fun parseClocOutput(output: String): RootStatistics {
        val lines = output.lines()
        var loc = 0
        var blankLoc = 0
        var commentLoc = 0
        var codeLoc = 0
        var ktFiles = 0

        for (line in lines) {
            if (line.contains("Kotlin")) {
                val parts = line.split(Regex("\\s+"))
                if (parts.size >= 5) {
                    ktFiles = parts[1].toIntOrNull() ?: 0
                    blankLoc = parts[2].toIntOrNull() ?: 0
                    commentLoc = parts[3].toIntOrNull() ?: 0
                    codeLoc = parts[4].toIntOrNull() ?: 0
                    loc = blankLoc + commentLoc + codeLoc
                }
            }
        }
        return RootStatistics(loc, blankLoc, commentLoc, codeLoc, ktFiles)
    }

    override fun toString(): String = innerTester.toString()
}

operator fun RootStatistics.plusAssign(other: RootStatistics) {
    this.loc += other.loc
    this.blankLoc += other.blankLoc
    this.commentLoc += other.commentLoc
    this.codeLoc += other.codeLoc
    this.ktFiles += other.ktFiles
}

fun <C : IJDDContext, T : DDItem> PropertyTester<C, T>.withStatistics(): PropertyTester<C, T> =
    PropertyTesterWithStatistics(this)
