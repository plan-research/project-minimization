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
import com.intellij.openapi.util.Disposer
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

    /**
     * Checks the compilation of the given project, ensuring it has the necessary Gradle tasks and runs them.
     *
     * @param context The context to check compilation for.
     * @return `List<BuildEvent>` if the compilation has been failed by kotlinc.
     * Each [BuildEvent][com.intellij.build.events.BuildEvent] contains some error
     *
     * or [CompilationPropertyCheckerError] if the compilation has been successful or some other error occurred
     */
    override suspend fun checkCompilation(context: IJDDContext): CompilationResult = either {
        val gradleTasks = extractGradleTasks(context).bind()
        // If not gradle tasks were found ⇒ this is not a Gradle project
        ensure(gradleTasks.isNotEmpty()) { CompilationPropertyCheckerError.InvalidBuildSystem }
        val buildTask = gradleTasks.findOrFail("build").bind()
        val cleanTask = gradleTasks.findOrFail("clean").bind()

        val cleanResult = runTask(context, cleanTask).bind()
        ensure(cleanResult.exitCode == 0) {
            CompilationPropertyCheckerError.BuildSystemFail(
                cause = IllegalStateException(
                    "Clean task failed: ${cleanResult.stdOut}",
                ),
            )
        }

        val buildResult = runTask(context, buildTask).bind()
        ensure(buildResult.exitCode != 0) { CompilationPropertyCheckerError.CompilationSuccess }
        parseResults("test-build-${context.indexProject.name}", buildResult)
    }

    /**
     * Executes a Gradle task for the given project and returns the result or an error if the task fails.
     *
     * Basically just contains a lot of boilerplate code to make IDEA work.
     * FIXME: Extract it to another module
     *
     * @param context Context where the Gradle task will be run.
     * @param task The specific GradleTask to be executed within the project.
     * @return Either a [CompilationPropertyCheckerError] if an error occurs during task execution,
     *         or a [GradleConsoleRunResult] containing the results of the task execution.
     */
    private suspend fun runTask(
        context: IJDDContext,
        task: GradleTask,
    ): Either<CompilationPropertyCheckerError, GradleConsoleRunResult> = either {
        val endProcessDisposable = Disposer.newDisposable()

        val processAdapter = GradleRunProcessAdapter()
        val configuration = buildConfiguration(context, task)
        val executor = DefaultRunExecutor.getRunExecutorInstance()
        val executionEnvironment =
            buildEnvironment(context, executor, configuration, processAdapter, endProcessDisposable)

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

    private fun buildConfiguration(
        context: IJDDContext,
        task: GradleTask,
    ): GradleRunConfiguration {
        val configurationFactory = GradleExternalTaskConfigurationType.getInstance().factory
        val configuration = GradleRunConfiguration(context.indexProject, configurationFactory, "Gradle Test Project Compilation")
        configuration.settings.apply {
            externalSystemIdString = GradleConstants.SYSTEM_ID.id
            taskNames = listOf(task.path)
            externalProjectPath = context.projectDir.path
            isPassParentEnvs = true
            scriptParameters = "--quiet"
        }
        return configuration
    }

    private fun buildEnvironment(
        context: IJDDContext,
        executor: Executor,
        configuration: GradleRunConfiguration,
        processAdapter: GradleRunProcessAdapter,
        endProcessDisposable: Disposable,
    ): ExecutionEnvironment =
        ExecutionEnvironmentBuilder
            .create(executor, configuration)
            .target(ExecutionTargetManager.getActiveTarget(context.indexProject))
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

    private fun extractGradleTasks(context: IJDDContext) = either {
        val gradleExecutionHelper = GradleExecutionHelper()
        catch(
            block = {
                gradleExecutionHelper.execute(context.projectDir.path, null) { connection ->
                    connection.action()
                    val gradleModel = connection.model(GradleProject::class.java).get()
                    gradleModel.tasks.toList()
                }
            },
            catch = { it: Throwable -> raise(CompilationPropertyCheckerError.BuildSystemFail(cause = it)) },
        )
    }

    private fun List<GradleTask>.findOrFail(name: String) = either {
        val task = this@findOrFail.firstOrNull { it.name == name }
        ensureNotNull(task) { CompilationPropertyCheckerError.InvalidBuildSystem }
        task
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

        // TODO: somehow report failed to parsed errors

        return IdeaCompilationException(
            parsedErrors
                .mapNotNull { it.getOrNull() },
        )
    }
}
