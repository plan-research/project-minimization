package org.plan.research.minimization.plugin.logging

import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.plugin.model.IJPropertyTester
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.item.IJDDItem
import org.plan.research.minimization.plugin.model.monad.SnapshotMonad
import org.plan.research.minimization.plugin.services.RootsManagerService

import ch.usi.si.seart.cloc.CLOC
import ch.usi.si.seart.cloc.CLOCException
import com.intellij.openapi.components.service
import mu.KotlinLogging

import java.nio.file.Path
import java.nio.file.Paths

import kotlin.io.path.exists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

data class RootStatistics(
    var loc: Int = 0,
    var blankLoc: Int = 0,
    var commentLoc: Int = 0,
    var codeLoc: Int = 0,
    var ktFiles: Int = 0,
)

class PropertyTesterWithStatistics<C : IJDDContext, T : IJDDItem>(
    private val innerTester: IJPropertyTester<C, T>,
) : IJPropertyTester<C, T> {
    private var isFirst: Boolean = true
    private val logger = KotlinLogging.logger {}

    context(SnapshotMonad<C>)
    override suspend fun test(retainedItems: List<T>, deletedItems: List<T>): PropertyTestResult {
        if (isFirst) {
            analyzeProject(context)
            isFirst = false
        }
        val result = innerTester.test(retainedItems, deletedItems)
        result.onRight { _ -> analyzeProject(context) }
        return result
    }

    private suspend fun analyzeProject(context: C) {
        val roots = service<RootsManagerService>().findPossibleRoots(context)
        val projectStat = RootStatistics()
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

    private suspend fun statisticsForPath(root: Path): RootStatistics {
        val clocOutput = runCloc(root.toString())
        return parseClocOutput(clocOutput)
    }

    private suspend fun runCloc(path: String): String = try {
        // Convert the input path to a Path object
        val targetPath: Path = Paths.get(path)

        // Use the Cloc library to analyze the target
        withContext(Dispatchers.IO) {
            CLOC.command()
                .timeout(30)  // Set the timeout in seconds
                .target(targetPath)  // Specify the target directory or file
                .linesByLanguage()  // Generate statistics grouped by programming language
                .toPrettyString()  // Convert the result to a formatted string
        }
    } catch (e: CLOCException) {
        "Error while running Cloc: ${e.message}"
    }

    private fun parseClocOutput(output: String): RootStatistics = try {
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
            ktFiles = ktFiles,
        )
    } catch (e: Exception) {
        logger.error("Error while running Cloc: ${e.message}")
        RootStatistics(
            loc = 0,
            blankLoc = 0,
            commentLoc = 0,
            codeLoc = 0,
            ktFiles = 0,
        )
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

fun <C : IJDDContext, T : IJDDItem> IJPropertyTester<C, T>.withStatistics(): IJPropertyTester<C, T> =
    PropertyTesterWithStatistics(this)
