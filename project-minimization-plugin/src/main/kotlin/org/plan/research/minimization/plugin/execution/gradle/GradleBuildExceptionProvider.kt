package org.plan.research.minimization.plugin.execution.gradle

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.intellij.build.output.KotlincOutputParser
import com.intellij.execution.ExecutionTargetManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import kotlinx.coroutines.CoroutineScope
import org.gradle.tooling.model.GradleTask
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionHelper
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.plan.research.minimization.plugin.errors.CompilationPropertyCheckerError
import org.plan.research.minimization.plugin.execution.IdeaCompilationException
import org.plan.research.minimization.plugin.execution.exception.KotlincException
import org.plan.research.minimization.plugin.execution.exception.KotlincExceptionUtils
import org.plan.research.minimization.plugin.model.BuildExceptionProvider

/**
 * An implementation of the [BuildExceptionProvider]
 * that checks the compilation status of a given Gradle-based project.
 *
 * The implementation for now is based on the `clean` and `compileKotlin` tasks.
 * However, TODO is to add a setting to make work with other tasks
 */
class GradleBuildExceptionProvider(private val cs: CoroutineScope) : BuildExceptionProvider {
    private val gradleOutputParser = KotlincOutputParser()

    /**
     * Checks the compilation of the given project, ensuring it has the necessary Gradle tasks and runs them.
     *
     * @param project The project to check compilation for.
     * @return `List<BuildEvent>` if the compilation has been failed by kotlinc.
     * Each [BuildEvent][com.intellij.build.events.BuildEvent] contains some error
     *
     * or [CompilationPropertyCheckerError] if the compilation has been successful or some other error occurred
     */
    override suspend fun checkCompilation(project: Project) = either {
        val gradleTasks = extractGradleTasks(project).bind()
        // If not gradle tasks were found ⇒ this is not a Gradle project
        ensure(gradleTasks.isNotEmpty()) { CompilationPropertyCheckerError.InvalidBuildSystem }
        val buildTask = gradleTasks.findOrFail("build").bind()
        val cleanTask = gradleTasks.findOrFail("clean").bind()

        val cleanResult = runTask(project, cleanTask).bind()
        ensure(cleanResult.exitCode == 0) {
            CompilationPropertyCheckerError.BuildSystemFail(
                cause = IllegalStateException(
                    "Clean task failed: ${cleanResult.stdOut}"
                )
            )
        }

        val buildResult = runTask(project, buildTask).bind()
        ensure(buildResult.exitCode != 0) { CompilationPropertyCheckerError.CompilationSuccess }
        parseResults("test-build-${project.name}", buildResult)
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
        task: GradleTask,
    ): Either<CompilationPropertyCheckerError, GradleConsoleRunResult> = either {
        val processAdapter = GradleRunProcessAdapter(cs)
        val configurationFactory = GradleExternalTaskConfigurationType.getInstance().factory
        val configuration = GradleRunConfiguration(project, configurationFactory, "Gradle Test Project Compilation")
        configuration.settings.apply {
            externalSystemIdString = GradleConstants.SYSTEM_ID.id
            taskNames = listOf(task.path)
            externalProjectPath = project.guessProjectDir()?.path
                ?: raise(CompilationPropertyCheckerError.InvalidBuildSystem)
            isPassParentEnvs = true
            scriptParameters = "--quiet"
        }

        val executor = DefaultRunExecutor.getRunExecutorInstance()

        val executionEnvironment = ExecutionEnvironmentBuilder
            .create(executor, configuration)
            .target(ExecutionTargetManager.getActiveTarget(project))
            .build()

        val runner = ProgramRunner.getRunner(executor.id, configuration)
        // Don't know any situation when it could happen
        ensureNotNull(runner) { CompilationPropertyCheckerError.InvalidBuildSystem }

        executionEnvironment.setCallback { descriptor ->
            descriptor
                .processHandler
                ?.addProcessListener(processAdapter)
        }
        writeAction {
            catch( // runner.execute can throw
                { runner.execute(executionEnvironment) },
                { raise(CompilationPropertyCheckerError.BuildSystemFail(it)) }
            )
        }
        processAdapter.getRunResult() // Wait until writeAction is done
    }

    private fun extractGradleTasks(project: Project) = either {
        val externalProjectPath = project.guessProjectDir()?.path
        // Fails then and only then, when this project is default
        ensureNotNull(externalProjectPath) { CompilationPropertyCheckerError.InvalidBuildSystem }
        val gradleExecutionHelper = GradleExecutionHelper()
        catch(
            {
                gradleExecutionHelper.execute(externalProjectPath, null) { connection ->
                    connection.action()
                    val gradleModel = connection.model(org.gradle.tooling.model.GradleProject::class.java).get()
                    gradleModel.tasks.toList()
                }
            },
            { it: Throwable -> raise(CompilationPropertyCheckerError.BuildSystemFail(cause = it)) }
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
                if (!gradleOutputParser.parse(line, outputReader) { add(KotlincExceptionUtils.parseException(it)) })
                    break
            }
        }
        // TODO: somehow report failed to parsed errors
        parsedErrors.forEach {
            when (it) {
                is Either.Left -> println("Failed to parsed exception. Error ${it.value}")
                else -> {}
            }
        }
        return IdeaCompilationException(
            parsedErrors
                .filterIsInstance<Either.Right<KotlincException>>()
                .map(Either.Right<KotlincException>::value)
        )
    }
}