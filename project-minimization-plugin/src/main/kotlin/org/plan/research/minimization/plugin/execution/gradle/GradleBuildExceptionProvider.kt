package org.plan.research.minimization.plugin.execution.gradle

import arrow.core.Either
import arrow.core.raise.*
import com.intellij.build.output.KotlincOutputParser
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import mu.KotlinLogging
import org.gradle.tooling.GradleConnectionException
import org.gradle.tooling.ResultHandler
import org.gradle.tooling.model.GradleProject
import org.gradle.tooling.model.GradleTask
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionHelper
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.plan.research.minimization.plugin.errors.CompilationPropertyCheckerError
import org.plan.research.minimization.plugin.execution.IdeaCompilationException
import org.plan.research.minimization.plugin.execution.exception.KotlincExceptionTranslator
import org.plan.research.minimization.plugin.model.BuildExceptionProvider
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.exception.CompilationResult
import java.io.ByteArrayOutputStream

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
     * @param project The project to check compilation for.
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
        parseResults("test-build-${context.projectDir.name}", buildResult)
    }

    private val logger = KotlinLogging.logger { }

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
        context: IJDDContext,
        task: GradleTask,
    ): Either<CompilationPropertyCheckerError, GradleConsoleRunResult> = either {
        val gradleExecutionHelper = GradleExecutionHelper()
        val std = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()
        val wait = MutableStateFlow(false)
        gradleExecutionHelper.execute(
            context.projectDir.path, null, ExternalSystemTaskId.create(
                GradleConstants.SYSTEM_ID, ExternalSystemTaskType.EXECUTE_TASK, context.originalProject
            ), null, null
        ) { connection ->
            connection.newBuild()
                .forTasks(task)
                .setStandardOutput(std)
                .setStandardError(err)
                .withArguments("--quiet")
                .run(object : ResultHandler<Void> {
                    override fun onComplete(p0: Void?) {
                        wait.value = true
                    }

                    override fun onFailure(p0: GradleConnectionException?) {
                        wait.value = true
                    }
                })
        }
        logger.info { "WAITING" }
        wait.first { it }
        logger.info { "DONE WAITING" }
        val error = err.toString(Charsets.UTF_8)
        logger.debug { "stdOut: ${std.toString(Charsets.UTF_8)}" }
        logger.debug { "err: ${err.toString(Charsets.UTF_8)}" }

        GradleConsoleRunResult(
            exitCode = if (error.isBlank()) 0 else 1,
            stdOut = std.toString(Charsets.UTF_8),
            stdErr = error,
            system = "",
        )
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
