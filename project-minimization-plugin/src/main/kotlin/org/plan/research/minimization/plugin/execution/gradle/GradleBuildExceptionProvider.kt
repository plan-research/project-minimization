package org.plan.research.minimization.plugin.execution.gradle

import org.plan.research.minimization.plugin.errors.CompilationPropertyCheckerError
import org.plan.research.minimization.plugin.execution.IdeaCompilationException
import org.plan.research.minimization.plugin.execution.exception.KotlincErrorSeverity
import org.plan.research.minimization.plugin.execution.exception.KotlincException
import org.plan.research.minimization.plugin.execution.exception.KotlincExceptionTranslator
import org.plan.research.minimization.plugin.execution.exception.ParseKotlincExceptionResult
import org.plan.research.minimization.plugin.execution.gradle.GradleConsoleRunResult.Companion.EXIT_CODE_FAIL
import org.plan.research.minimization.plugin.execution.gradle.GradleConsoleRunResult.Companion.EXIT_CODE_OK
import org.plan.research.minimization.plugin.model.BuildExceptionProvider
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.exception.CompilationResult
import org.plan.research.minimization.plugin.services.MinimizationPluginSettings

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.intellij.build.output.KotlincOutputParser
import com.intellij.openapi.application.readAndWriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportSequentialProgress
import mu.KotlinLogging
import org.gradle.tooling.GradleConnectionException
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.ResultHandler
import org.gradle.tooling.internal.consumer.DefaultCancellationTokenSource
import org.gradle.tooling.model.GradleProject
import org.gradle.tooling.model.GradleTask
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionHelper
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants

import java.io.ByteArrayOutputStream
import java.io.File

import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

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
        withBackgroundProgress(context.indexProject, "Gradle building", cancellable = false) {
            reportSequentialProgress(5) { reporter ->
                try {
                    reporter.itemStep("Deleting cache") { deleteCache(context) }

                    val gradleTasks =
                        reporter.itemStep("Extracting gradle tasks") { extractGradleTasks(context).bind() }
                    // If not gradle tasks were found ⇒ this is not a Gradle project
                    ensure(gradleTasks.isNotEmpty()) { CompilationPropertyCheckerError.InvalidBuildSystem }
                    val settings = context.originalProject.service<MinimizationPluginSettings>()
                    val buildTask = gradleTasks.findOrFail(settings.state.gradleTask).bind()
                    val buildOptions = settings.state.gradleOptions
                    val cleanTask = gradleTasks.findOrFail("clean").bind()

                    val cleanResult = reporter.itemStep("Running clean task") { runTask(context, cleanTask).bind() }
                    ensure(cleanResult.exitCode == EXIT_CODE_OK) {
                        CompilationPropertyCheckerError.BuildSystemFail(
                            cause = IllegalStateException(
                                "Clean task failed: ${cleanResult.stdOut}",
                            ),
                        )
                    }

                    val buildResult =
                        reporter.itemStep("Running build task") { runTask(context, buildTask, buildOptions).bind() }
                    ensure(buildResult.exitCode != EXIT_CODE_OK) { CompilationPropertyCheckerError.CompilationSuccess }
                    reporter.itemStep("Parsing results") {
                        parseResults("test-build-${context.indexProject.name}", buildResult)
                    }
                } finally {
                    context.projectDir.refresh(false, true)
                }
            }
        }
    }

    private suspend fun deleteCache(context: IJDDContext) {
        readAndWriteAction {
            val files = buildList {
                VfsUtil.iterateChildrenRecursively(context.projectDir, null) { file ->
                    if (file.name == ".gradle") {
                        add(file)
                    }
                    true
                }
            }
            if (files.isNotEmpty()) {
                writeAction {
                    files.forEach {
                        logger.trace { "Deleting ${it.path}" }
                        try {
                            it.delete(null)
                        } catch (e: Throwable) {
                            logger.error(e) { "Error while deleting ${it.path}" }
                        }
                    }
                }
            } else {
                value(Unit)
            }
        }
    }

    private fun buildGradleExecutionSettings(context: IJDDContext): GradleExecutionSettings? {
        // Retrieve the Gradle settings from the opened project
        val gradleSettings = GradleSettings.getInstance(context.indexProject)

        // Since the external project isn't linked, we'll use settings from the opened project's first linked project
        val defaultProjectSettings = gradleSettings.linkedProjectsSettings.firstOrNull() ?: return null

        // Create the GradleExecutionSettings instance
        val executionSettings = GradleExecutionSettings(
            defaultProjectSettings.gradleHome,
            gradleSettings.serviceDirectoryPath,
            defaultProjectSettings.distributionType ?: DistributionType.DEFAULT_WRAPPED,
            false,
        )

        // Set the Gradle JVM (Java home)
        val gradleJvm = defaultProjectSettings.gradleJvm
        logger.debug { "Gradle JVM: $gradleJvm" }
        gradleJvm?.let {
            val sdk = ExternalSystemJdkUtil.getJdk(context.indexProject, gradleJvm)
            logger.debug { "Found sdk: $sdk" }
            sdk?.let {
                executionSettings.javaHome = sdk.homePath
                logger.debug {
                    """
                       Target jvm: ${sdk.name},
                        homePath: ${sdk.homePath},
                        version: ${sdk.versionString},
                        type: ${sdk.sdkType.name}
                    """.trimIndent()
                }
            }
        }

        // Return the configured execution settings
        return executionSettings.also {
            logger.debug {
                "Execution gradle settings: $it"
            }
        }
    }

    @Suppress("TYPE_ALIAS")
    private suspend inline fun <T> executeWithGradleConnection(
        context: IJDDContext,
        crossinline block: (ProjectConnection, Continuation<T>, String?) -> Unit,
    ): T {
        val gradleExecutionHelper = GradleExecutionHelper()
        val settings = buildGradleExecutionSettings(context)
        val cancellation = DefaultCancellationTokenSource()
        val taskId = ExternalSystemTaskId.create(
            GradleConstants.SYSTEM_ID, ExternalSystemTaskType.EXECUTE_TASK, context.indexProject,
        )
        return suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { cancellation.cancel() }
            gradleExecutionHelper.execute(
                context.projectDir.path, settings,
                taskId, null, cancellation.token(),
            ) { connection ->
                block(connection, cont, settings?.javaHome)
            }
        }
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
        task: ExecutableGradleTask,
        options: List<String> = emptyList(),
    ): Either<CompilationPropertyCheckerError, GradleConsoleRunResult> = either {
        logger.info { "Run gradle task: ${task.executableName}" }
        logger.info { "Additional gradle task options: ${options.joinToString(" ")}" }

        val std = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()

        val exitCode = executeWithGradleConnection(context) { connection, cont, javaHome ->
            connection.newBuild()
                .forTasks(task.executableName)
                .setStandardOutput(std)
                .setStandardError(err)
                .setJavaHome(javaHome?.let { File(it) })
                .withArguments(*defaultArguments, *options.toTypedArray())
                .run(object : ResultHandler<Void> {
                    override fun onComplete(result: Void?) {
                        cont.resume(EXIT_CODE_OK)
                    }

                    override fun onFailure(exception: GradleConnectionException) {
                        logger.trace(exception) { "Gradle task ${task.executableName} failure" }
                        cont.resume(EXIT_CODE_FAIL)
                    }
                })
        }

        val error = err.toString(Charsets.UTF_8)
        val stdout = std.toString(Charsets.UTF_8)
        logger.trace { "STD: $stdout" }
        logger.trace { "ERR: $error" }

        GradleConsoleRunResult(
            exitCode = exitCode,
            stdOut = stdout,
            stdErr = error,
        )
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

    private suspend fun extractGradleTasks(context: IJDDContext) = either {
        catch(
            block = {
                val model = executeWithGradleConnection(context) { connection, cont, javaHome ->
                    connection.model(GradleProject::class.java)
                        .setJavaHome(javaHome?.let { File(it) })
                        .get(object : ResultHandler<GradleProject> {
                            override fun onComplete(result: GradleProject) {
                                cont.resume(result)
                            }

                            override fun onFailure(exception: GradleConnectionException) {
                                cont.resumeWithException(exception)
                            }
                        })
                }
                extractGradleTasksFromModel(model)
            },
            catch = {
                logger.error(it) { "Error while extracting gradle tasks" }
                raise(CompilationPropertyCheckerError.BuildSystemFail(cause = it))
            },
        )
    }

    private fun Map<String, GradleTask>.findOrFail(name: String) = either {
        if (name.startsWith(':')) {
            get(name)?.let {
                ExecutableGradleTask.fromTask(it)
            } ?: raise(CompilationPropertyCheckerError.InvalidBuildSystem)
        } else {
            ensure(values.any { it.path.endsWith(":$name") }) { CompilationPropertyCheckerError.InvalidBuildSystem }
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

        logParsedErrors(parsedErrors)

        return IdeaCompilationException(
            parsedErrors
                .mapNotNull { it.getOrNull() }
                .filter { e ->
                    (e as? KotlincException.GeneralKotlincException)?.let {
                        it.severity == KotlincErrorSeverity.ERROR || it.severity == KotlincErrorSeverity.UNKNOWN
                    } ?: true
                },
        )
    }

    private fun logParsedErrors(parsedErrors: List<ParseKotlincExceptionResult>) {
        if (logger.isTraceEnabled) {
            logger.trace {
                "Parsed errors:\n${
                    parsedErrors.joinToString("\n") { error ->
                            error.fold(
                                ifLeft = { "Parsing failed with error:\n$it" },
                                ifRight = { "Error parsed successfully:\n$it" },
                            )
                        }
                }"
            }
        } else if (logger.isDebugEnabled) {
            logger.debug {
                "Parsed errors:\n${
                    parsedErrors.joinToString("\n") { error ->
                            error.fold(
                                ifLeft = { "Parsing failed with error:\n$it" },
                                ifRight = { "Error parsed successfully:\n${it::class.simpleName}" },
                            )
                        }
                }"
            }
        }
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

    companion object {
        val defaultArguments = arrayOf("--no-configuration-cache", "--no-build-cache", "--quiet")
    }
}
