package org.plan.research.minimization.plugin.logging

import ch.usi.si.seart.cloc.CLOC
import ch.usi.si.seart.cloc.CLOCException
import com.intellij.openapi.components.service
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.services.RootsManagerService
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

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
    private var isFirst: Boolean = true
    private val logger = KotlinLogging.logger {}

    override suspend fun test(context: C, items: List<T>): PropertyTestResult<C> {
        if (isFirst) {analyzeProject(context); isFirst = false }
        val result = innerTester.test(context, items)
        result.onRight { value -> analyzeProject(value) }
        return result
    }

    private fun analyzeProject(context: C) {
        val roots = service<RootsManagerService>().findPossibleRoots(context)
        var projectStat = RootStatistics()
        val projectDirPath = context.projectDir.toNioPath()
        for (root in roots) {
            val fullPath = projectDirPath.resolve(root)

            if (fullPath.exists()) {
                val stat = statisticsForPath(fullPath)
                projectStat += stat
            }
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
        return try {
            // Convert the input path to a Path object
            val targetPath: Path = Paths.get(path)

            // Use the Cloc library to analyze the target
            CLOC.command()
                .timeout(30) // Set the timeout in seconds
                .target(targetPath) // Specify the target directory or file
                .linesByLanguage() // Generate statistics grouped by programming language
                .toPrettyString() // Convert the result to a formatted string
        } catch (e: CLOCException) {
            "Error while running Cloc: ${e.message}"
        }
    }

    private fun parseClocOutput(output: String): RootStatistics {
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val clocStatistics = json.decodeFromString<ClocStatistics>(output)

            val kotlinStats = clocStatistics.kotlin

            val loc = kotlinStats?.let { it.blank + it.comment + it.code } ?: 0
            val blankLoc = kotlinStats?.blank ?: 0
            val commentLoc = kotlinStats?.comment ?: 0
            val codeLoc = kotlinStats?.code ?: 0
            val ktFiles = kotlinStats?.nFiles ?: 0

            RootStatistics(
                loc = loc,
                blankLoc = blankLoc,
                commentLoc = commentLoc,
                codeLoc = codeLoc,
                ktFiles = ktFiles
            )
        } catch (e: Exception) {
            logger.error("Error while running Cloc: ${e.message}")
            RootStatistics(
                loc = 0,
                blankLoc = 0,
                commentLoc = 0,
                codeLoc = 0,
                ktFiles = 0
            )
        }
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
