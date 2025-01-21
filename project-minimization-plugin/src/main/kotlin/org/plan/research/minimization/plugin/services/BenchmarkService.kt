package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.benchmark.BenchmarkConfig
import org.plan.research.minimization.plugin.benchmark.BenchmarkProject
import org.plan.research.minimization.plugin.benchmark.BuildSystemType
import org.plan.research.minimization.plugin.benchmark.ProjectModulesType
import org.plan.research.minimization.plugin.settings.loadStateFromFile

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.raise.option
import com.charleskorn.kaml.Yaml
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.findFile
import com.intellij.openapi.vfs.readText
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportSequentialProgress
import mu.KotlinLogging

import java.io.File
import java.nio.file.Path

import kotlin.io.path.Path
import kotlinx.coroutines.*

@Service(Service.Level.PROJECT)
class BenchmarkService(private val rootProject: Project, private val cs: CoroutineScope) {
    private val logger = KotlinLogging.logger {}

    fun benchmark() = cs.launch {
        logger.info { "Start benchmark Action" }

        logger.info { "Read Config" }
        val config = readConfig().getOrNull() ?: return@launch

        val filteredProjects = config
            .projects
            .filter { it.isSuitableForGradleBenchmarking(allowAndroid = false) }  // FIXME
        withBackgroundProgress(rootProject, "Running Minimization Benchmark") {
            reportSequentialProgress(filteredProjects.size) { reporter ->
                filteredProjects.forEach { project ->
                    reporter.itemStep("Minimizing ${project.name}") {
                        project.process()
                    }
                }
            }
        }

        runCleaningActions()
    }

    private suspend fun runCleaningActions() {
        logger.info { "Running cleaning actions..." }

        // Path to the root project
        val rootFile = rootProject.guessProjectDir()?.toNioPath()?.toFile() ?: run {
            logger.error { "Root project path is null, skipping cleaning actions" }
            return
        }

        // Execute first cleaning scripts
        withContext(Dispatchers.IO) {
            executeShellCommand(
                "find . -name \"minimization-project-snapshots\" -type d -exec rm -rf '{}' '+'",
                rootFile,
            )

            executeShellCommand(
                "for i in \$(ls projects/); do\n" +
                    "    echo \"Cleaning \$i\"\n" +
                    "    cd \"projects/\$i\"\n" +
                    "    ./gradlew clean\n" +
                    "    cd ../..\n" +
                    "done",
                rootFile,
            )
        }
    }

    private fun executeShellCommand(command: String, workingFile: File) {
        try {
            val process = ProcessBuilder("/bin/sh", "-c", command)
                .directory(workingFile)
                .redirectErrorStream(true)
                .start()

            process.inputStream.bufferedReader().use { reader ->
                reader.lines().forEach { logger.info { it } }
            }

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                logger.error { "Command failed with exit code $exitCode: $command" }
            } else {
                logger.info { "Command executed successfully: $command" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error while executing command: $command" }
        }
    }

    private suspend fun closeProject(project: Project) {
        ProjectManagerEx.getInstanceEx().forceCloseProjectAsync(project)
    }

    private suspend fun readConfig(): Option<BenchmarkConfig> = option {
        val projectRoot = ensureNotNull(rootProject.guessProjectDir())
        val configurationFile = ensureNotNull(projectRoot.findFile("config.yaml"))
        val content = withContext(Dispatchers.IO) { configurationFile.readText() }
        Yaml.default.decodeFromString(BenchmarkConfig.serializer(), content)
    }

    private fun getBenchmarkProjectRoot(project: BenchmarkProject): Option<Path> = option {
        val root = ensureNotNull(rootProject.guessProjectDir()?.toNioPath())
        root.resolve(Path(project.path))
    }

    private suspend fun openBenchmarkProject(project: BenchmarkProject): Option<Project> = option {
        val root = getBenchmarkProjectRoot(project).bind()
        try {
            val benchmarkProject = service<ProjectOpeningService>().openProject(root)
            ensureNotNull(benchmarkProject)
        } catch (e: Throwable) {
            e.printStackTrace()
            raise(None)
        }
    }

    private suspend fun BenchmarkProject.process(): Project? {
        logger.info { "Launch benchmark: ${this@process.name}" }

        val openedProject = openBenchmarkProject(this@process).getOrNull() ?: return null

        if (openedProject.isDisposed) {
            throw IllegalStateException("Project is already disposed")
        }

        var resultProject: Project? = null

        try {
            setMinimizationSettings(openedProject, this@process)
            val minimizationService = openedProject.service<MinimizationService>()

            logger.info { "Start Minimization Action" }
            val result = withBackgroundProgress(openedProject, "Minimizing project") {
                minimizationService.minimizeProjectAsync()
            }
            logger.info { "End Minimization Action" }

            resultProject = when (result) {
                is Either.Right -> result.value.project
                is Either.Left -> null
            }

            return resultProject
        } finally {
            withContext(Dispatchers.EDT + NonCancellable) {
                logger.info { "Close project: ${this@process.name}" }
                if (!openedProject.isDisposed) {
                    closeProject(openedProject)
                }
                resultProject?.let { project ->
                    if (!project.isDisposed) {
                        closeProject(project)
                    }
                }
            }
        }
    }

    private fun setMinimizationSettings(project: Project, benchmarkProject: BenchmarkProject) {
        logger.info { "Set settings from file: ${benchmarkProject.settingsFile}" }

        val settingsFileRoot = benchmarkProject.settingsFile?.let { settingsFile ->
            rootProject.guessProjectDir()?.toNioPath()?.resolve(settingsFile)
        }

        settingsFileRoot?.let { loadStateFromFile(project, it.toString()) }
    }

    /**
     * Function to transform a reproducing script into Gradle's task name.
     * NB: this script assumes that all reproducing scripts
     *  * uses Gradle
     *  * uses a single task (or a single task + `clean` task)
     *  * do not do extra actions
     */
    private fun BenchmarkProject.isSuitableForGradleBenchmarking(allowAndroid: Boolean): Boolean =
        (allowAndroid || this.extra?.tags?.contains("android") != true) &&
            this.buildSystem.type == BuildSystemType.GRADLE &&
            this.modules == ProjectModulesType.SINGLE
}
