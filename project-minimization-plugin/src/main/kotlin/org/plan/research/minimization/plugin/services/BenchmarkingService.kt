package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.benchmark.BenchmarkConfig
import org.plan.research.minimization.plugin.benchmark.BenchmarkProject
import org.plan.research.minimization.plugin.benchmark.BenchmarkResultSubscriber
import org.plan.research.minimization.plugin.benchmark.BuildSystemType
import org.plan.research.minimization.plugin.errors.MinimizationError
import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.state.DDStrategy
import org.plan.research.minimization.plugin.model.state.HierarchyCollectionStrategy
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.raise.option
import com.charleskorn.kaml.Yaml
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.findFile
import com.intellij.openapi.vfs.readText
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.ProgressReporter
import com.intellij.platform.util.progress.reportProgress

import java.nio.file.Path

import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class BenchmarkingService(private val rootProject: Project, private val cs: CoroutineScope) {
    fun benchmark(adapter: BenchmarkResultSubscriber) = cs.launch {
        val config = readConfig().getOrNull()
        config ?: run {
            adapter.onConfigCreationError()
            return@launch
        }
        val filteredProjects = config
            .projects
            .filter { it.isSuitableForGradleBenchmarking(allowAndroid = false) }  // FIXME
        withBackgroundProgress(rootProject, "Running Minimization Benchmark") {
            reportProgress(filteredProjects.size) { reporter ->
                processProjects(filteredProjects, reporter)
                    .cancellable()
                    .collect { (result, projectConfig) ->
                        result.fold(
                            ifLeft = { adapter.onFailure(it, projectConfig) },
                            ifRight = {
                                adapter.onSuccess(it, projectConfig)
                                closeProject(it)
                            },
                        )
                    }
            }
        }
    }

    private suspend fun closeProject(project: Project) = withContext(Dispatchers.EDT) {
        ProjectManager.getInstance().closeAndDispose(project)
    }

    private suspend fun readConfig(): Option<BenchmarkConfig> = option {
        val projectRoot = rootProject.guessProjectDir() ?: raise(None)
        val configurationFile = projectRoot.findFile("config.yaml") ?: raise(None)
        val content = withContext(Dispatchers.IO) { configurationFile.readText() }
        Yaml.default.decodeFromString(BenchmarkConfig.serializer(), content)
    }

    private fun getBenchmarkProjectRoot(project: BenchmarkProject): Option<Path> = option {
        val root = rootProject.guessProjectDir()?.toNioPath() ?: raise(None)
        root.resolve(Path(project.path))
    }

    private suspend fun openBenchmarkProject(project: BenchmarkProject): Option<Project> = option {
        val root = getBenchmarkProjectRoot(project).bind()
        withContext(Dispatchers.EDT) {
            ProjectUtil.openOrImportAsync(root) ?: raise(None)
        }
    }

    private suspend fun processProjects(projects: List<BenchmarkProject>, reporter: ProgressReporter) = projects
        .asFlow()
        .map { project ->
            reporter.itemStep("Minimizing ${project.name}") {
                val gradleBuildTask = loadReproduceScript(project) ?: "build"
                val openedProject = openBenchmarkProject(project).getOrNull() ?: return@itemStep null
                try {
                    setMinimizationSettings(openedProject, gradleBuildTask)
                    val minimizationService = openedProject.service<MinimizationService>()
                    val result = minimizationService.minimizeProject(openedProject, coroutineContext).await()
                    BenchmarkMinimizationResult(result, project)
                } finally {
                    closeProject(openedProject)
                }
            }
        }
        .filterNotNull()

    private fun setMinimizationSettings(project: Project, runTask: String) {
        project
            .service<MinimizationPluginSettings>()
            .state
            .apply {
                stages = mutableListOf(
                    FileLevelStage(
                        hierarchyCollectionStrategy = HierarchyCollectionStrategy.FILE_TREE,
                        ddAlgorithm = DDStrategy.DD_MIN,  // For now (18.10.2024) works better. FIXME
                    ),
                )
                gradleCompilationTask = runTask
            }
    }

    private suspend fun loadReproduceScript(project: BenchmarkProject): String? {
        val root = rootProject.guessProjectDir()?.toNioPathOrNull() ?: return null
        val reproduceScript = root.resolve(Path(project.reproduceScriptPath))
        val content = withContext(Dispatchers.IO) { reproduceScript.readText() }
        return getGradleBuildTask(content)
    }

    /**
     * Function to transform a reproducing script into Gradle's task name.
     * NB: this script assumes that all reproducing scripts
     *  * uses Gradle
     *  * uses a single task (or a single task + `clean` task)
     *  * do not do extra actions
     */
    private fun getGradleBuildTask(reproduceScript: String): String? {
        val gradleLine = reproduceScript.lines().find { it.startsWith("./gradlew") } ?: return null
        val tasks = gradleLine
            .split(" ")
            .asSequence()
            .drop(1)
            .map(String::trim)
            .filterNot { "clean" in it }
            .filterNot { it.startsWith("\"") }
            .filterNot { it.startsWith("--") }
        return tasks.firstOrNull()
    }

    private fun BenchmarkProject.isSuitableForGradleBenchmarking(allowAndroid: Boolean): Boolean =
        (allowAndroid || this.extra?.tags?.contains("android") != true) &&
            this.buildSystem.type == BuildSystemType.GRADLE

    private data class BenchmarkMinimizationResult(
        val result: Either<MinimizationError, Project>,
        val projectConfig: BenchmarkProject,
    )
}
