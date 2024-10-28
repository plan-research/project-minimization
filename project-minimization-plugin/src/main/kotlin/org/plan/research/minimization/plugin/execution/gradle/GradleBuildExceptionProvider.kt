package org.plan.research.minimization.plugin.execution.gradle

import org.plan.research.minimization.plugin.errors.CompilationPropertyCheckerError
import org.plan.research.minimization.plugin.execution.IdeaCompilationException
import org.plan.research.minimization.plugin.execution.exception.KotlincExceptionTranslator
import org.plan.research.minimization.plugin.model.BuildExceptionProvider
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.exception.CompilationResult

import arrow.core.Either
import arrow.core.raise.*
import com.intellij.build.output.KotlincOutputParser
import com.intellij.execution.ExecutionTargetManager
import com.intellij.execution.Executor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Disposer
import mu.KotlinLogging
import org.gradle.tooling.model.GradleProject
import org.gradle.tooling.model.GradleTask
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionHelper
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.jetbrains.plugins.gradle.util.GradleConstants

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * An implementation of the [BuildExceptionProvider]
 * that checks the compilation status of a given Gradle-based project.
 *
 * The implementation for now is based on the `clean` and `compileKotlin` tasks.
 * However, TODO is to add a setting to make work with other tasks
 */
class GradleBuildExceptionProvider : BuildExceptionProvider {
    private val gradleOutputParser = KotlincOutputParser()
    private val kotlincExceptionTranslator = KotlincExceptionTranslator()
    private val logger = KotlinLogging.logger { }

    /**
     * Checks the compilation of the given project, ensuring it has the necessary Gradle tasks and runs them.
     *
     * @param context The project's context to check compilation for.
     * @return `List<BuildEvent>` if the compilation has been failed by kotlinc.
     * Each [BuildEvent][com.intellij.build.events.BuildEvent] contains some error
     *
     * or [CompilationPropertyCheckerError] if the compilation has been successful or some other error occurred
     */
    override suspend fun checkCompilation(context: IJDDContext): CompilationResult = either {
        val project = context.project
        val gradleTasks = extractGradleTasks(project).bind()
        // If not gradle tasks were found ⇒ this is not a Gradle project
        ensure(gradleTasks.isNotEmpty()) { CompilationPropertyCheckerError.InvalidBuildSystem }
        val buildTask = gradleTasks.findOrFail("build").bind()
        val cleanTask = gradleTasks.findOrFail("clean").bind()

        val cleanResult = runTask(project, cleanTask).bind()
        ensure(cleanResult.exitCode == 0) {
            CompilationPropertyCheckerError.BuildSystemFail(
                cause = IllegalStateException(
                    "Clean task failed: ${cleanResult.stdOut}",
                ),
            )
        }

        val buildResult = runTask(project, buildTask).bind()
        ensure(buildResult.exitCode != 0) { CompilationPropertyCheckerError.CompilationSuccess }
        parseResults("test-build-${project.name}-${context.projectDir.name}", buildResult)
    }

    /**
     * Executes a Gradle task for the given project and returns the result or an error if the task fails.
     *
     * Basically just contains a lot of boilerplate code to make IDEA work.
     * FIXME: Extract it to another module
     *
     * @param project Project instance where the Gradle task will be run.
     * @param task The specific GradleTask to be executed within the project.
     * @return Either a [CompilationPropertyCheckerError] if an error occurs during task execution,
     *         or a [GradleConsoleRunResult] containing the results of the task execution.
     */
    private suspend fun runTask(
        project: Project,
        task: ExecutableGradleTask,
    ): Either<CompilationPropertyCheckerError, GradleConsoleRunResult> = either {
        val endProcessDisposable = Disposer.newDisposable()

        val processAdapter = GradleRunProcessAdapter()
        val configuration = buildConfiguration(project, task)
        val executor = DefaultRunExecutor.getRunExecutorInstance()
        val executionEnvironment =
            buildEnvironment(project, executor, configuration, processAdapter, endProcessDisposable)

        val runner = ProgramRunner.getRunner(executor.id, configuration)
        // Don't know any situation when it could happen
        ensureNotNull(runner) { CompilationPropertyCheckerError.InvalidBuildSystem }

        try {
            withContext(Dispatchers.EDT) {
                catch(
                    block = { runner.execute(executionEnvironment) },
                    catch = { raise(CompilationPropertyCheckerError.BuildSystemFail(it)) },
                )
            }
            processAdapter.getRunResult()  // Wait until writeAction is done
        } finally {
            Disposer.dispose(endProcessDisposable)
        }
    }

    private fun Raise<CompilationPropertyCheckerError>.buildConfiguration(
        project: Project,
        task: ExecutableGradleTask,
    ): GradleRunConfiguration {
        val configurationFactory = GradleExternalTaskConfigurationType.getInstance().factory
        val configuration = GradleRunConfiguration(project, configurationFactory, "Gradle Test Project Compilation")
        configuration.settings.apply {
            externalSystemIdString = GradleConstants.SYSTEM_ID.id
            taskNames = listOf(task.executableName)
            externalProjectPath = project.guessProjectDir()?.path
                ?: raise(CompilationPropertyCheckerError.InvalidBuildSystem)
            isPassParentEnvs = true
            scriptParameters = "--quiet --no-configuration-cache"
        }
        return configuration
    }

    private fun buildEnvironment(
        project: Project,
        executor: Executor,
        configuration: GradleRunConfiguration,
        processAdapter: GradleRunProcessAdapter,
        endProcessDisposable: Disposable,
    ): ExecutionEnvironment =
        ExecutionEnvironmentBuilder
            .create(executor, configuration)
            .target(ExecutionTargetManager.getActiveTarget(project))
            .build { descriptor ->
                descriptor.processHandler?.let {
                    it.addProcessListener(processAdapter, endProcessDisposable)
                    Disposer.register(endProcessDisposable) {
                        if (!it.isProcessTerminated && !it.isProcessTerminating) {
                            it.destroyProcess()
                        }
                    }
                }
            }

    private fun extractGradleTasksFromModel(gradleModel: GradleProject): Map<String, GradleTask> =
        buildMap {
            val queue = ArrayDeque<GradleProject>()
            queue.add(gradleModel)
            while (queue.isNotEmpty()) {
                val model = queue.removeFirst()
                putAll(model.tasks.map { it.path to it })
                queue.addAll(model.children)
            }
        }

    private fun extractGradleTasks(project: Project) = either {
        val externalProjectPath = project.guessProjectDir()?.path
        // Fails then and only then, when this project is default
        ensureNotNull(externalProjectPath) { CompilationPropertyCheckerError.InvalidBuildSystem }
        val gradleExecutionHelper = GradleExecutionHelper()
        catch(
            block = {
                gradleExecutionHelper.execute(externalProjectPath, null) { connection ->
                    connection.action()
                    val gradleModel = connection.model(GradleProject::class.java).get()
                    extractGradleTasksFromModel(gradleModel)
                }
            },
            catch = { it: Throwable -> raise(CompilationPropertyCheckerError.BuildSystemFail(cause = it)) },
        )
    }

    private fun Map<String, GradleTask>.findOrFail(name: String) = either {
        if (name.startsWith(':')) {
            get(name)?.let {
                ExecutableGradleTask.fromTask(it)
            } ?: raise(CompilationPropertyCheckerError.InvalidBuildSystem)
        } else {
            ensure(values.any { it.name == name }) { CompilationPropertyCheckerError.InvalidBuildSystem }
            ExecutableGradleTask.fromName(name)
        }
    }

    /**
     * Parses the results of a Gradle console run and constructs an [IdeaCompilationException] using the parsed errors.
     *
     * @param eventId The identifier for the event in the build process.
     * @param results The [GradleConsoleRunResult] containing the standard error output from the Gradle run.
     * @return An [IdeaCompilationException] containing a list of build errors.
     */
    private fun parseResults(eventId: String, results: GradleConsoleRunResult): IdeaCompilationException {
        val outputReader = StringBuildOutputInstantReader.create(eventId, results.stdErr)
        val parsedErrors = buildList {
            while (true) {
                val line = outputReader.readLine() ?: break
                if (!gradleOutputParser.parse(
                    line,
                    outputReader,
                ) { add(kotlincExceptionTranslator.parseException(it)) }
                ) {
                    break
                }
            }
        }

        logger.debug {
            "Parsed errors:\n${parsedErrors.joinToString("\n") { error ->
                error.fold(
                    ifLeft = { "Parsing failed with error:\n$it" },
                    ifRight = { "Error parsed successfully:\n$it" },
                )
            }}"
        }

        return IdeaCompilationException(
            parsedErrors
                .mapNotNull { it.getOrNull() },
        )
    }

    private sealed interface ExecutableGradleTask {
        val executableName: String

        data class ExactGradleTask(val task: GradleTask) : ExecutableGradleTask {
            override val executableName: String = task.path
        }

        data class GeneralGradleTask(val name: String) : ExecutableGradleTask {
            override val executableName: String = name
        }

        companion object {
            fun fromTask(task: GradleTask) = ExactGradleTask(task)
            fun fromName(name: String) = GeneralGradleTask(name)
        }
    }
}
