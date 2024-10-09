package org.plan.research.minimization.plugin.execution.gradle

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
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
import org.plan.research.minimization.plugin.model.CompilationPropertyChecker

class GradleCompilationPropertyChecker(private val cs: CoroutineScope) : CompilationPropertyChecker {
    override suspend fun checkCompilation(project: Project) = either {
        // FIXME: Error filtering
        val externalProjectPath = project.guessProjectDir()?.path
        ensureNotNull(externalProjectPath) { CompilationPropertyCheckerError.InvalidBuildSystem }
        val gradleExecutionHelper = GradleExecutionHelper()
        val gradleTasks = catch(
            {
                gradleExecutionHelper.execute(externalProjectPath, null) { connection ->
                    connection.action()
                    val gradleModel = connection.model(org.gradle.tooling.model.GradleProject::class.java).get()
                    gradleModel.tasks
                }
            },
            { it: Throwable -> raise(CompilationPropertyCheckerError.BuildSystemFail(cause = it)) }
        )
        ensure(gradleTasks.isNotEmpty()) { CompilationPropertyCheckerError.InvalidBuildSystem }
        val buildTask = gradleTasks.firstOrNull { it.name == "build" }
        val cleanTask = gradleTasks.firstOrNull { it.name == "clean" }
        ensureNotNull(cleanTask) { CompilationPropertyCheckerError.InvalidBuildSystem }
        ensureNotNull(buildTask) { CompilationPropertyCheckerError.InvalidBuildSystem }
        val cleanResult = runTask(project, cleanTask).bind()
        ensure(cleanResult.exitCode == 0) { CompilationPropertyCheckerError.BuildSystemFail(Throwable(cleanResult.output)) }

        val buildResult = runTask(project, buildTask).bind()
        ensure(buildResult.exitCode != 0) { CompilationPropertyCheckerError.CompilationSuccess }
        Throwable(buildResult.output)
    }

    private suspend fun runTask(
        project: Project,
        task: GradleTask,
    ): Either<CompilationPropertyCheckerError, GradleRunResult> = either {
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
        ensureNotNull(runner) { CompilationPropertyCheckerError.InvalidBuildSystem }

        executionEnvironment.setCallback { descriptor ->
            descriptor
                .processHandler
                ?.addProcessListener(processAdapter)
        }
        writeAction {
            catch(
                { runner.execute(executionEnvironment) },
                { raise(CompilationPropertyCheckerError.BuildSystemFail(it)) }
            )
        }
        processAdapter.getRunResult()
    }
}