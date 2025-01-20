package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.benchmark.BenchmarkConfig
import org.plan.research.minimization.plugin.benchmark.BenchmarkProject
import org.plan.research.minimization.plugin.benchmark.BuildSystemType
import org.plan.research.minimization.plugin.benchmark.ProjectModulesType
import org.plan.research.minimization.plugin.errors.MinimizationError
import org.plan.research.minimization.plugin.settings.loadStateFromFile

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.raise.catch
import arrow.core.raise.option
import com.charleskorn.kaml.Yaml
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.findFile
import com.intellij.openapi.vfs.readText
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportSequentialProgress

import java.nio.file.Path

import kotlin.coroutines.resume
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlinx.coroutines.*
import mu.KotlinLogging

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
    }

    private suspend fun closeProject(project: Project) = withContext(Dispatchers.EDT) {
        ProjectManager.getInstance().closeAndDispose(project)
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
            val project = service<ProjectOpeningService>().openProject(root)
            ensureNotNull(project)
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
