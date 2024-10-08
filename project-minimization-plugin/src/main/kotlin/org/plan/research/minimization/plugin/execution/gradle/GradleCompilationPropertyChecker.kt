package org.plan.research.minimization.plugin.execution.gradle

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.intellij.execution.ExecutionTargetManager
import com.intellij.execution.RunManager
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
        val gradleTasks = gradleExecutionHelper.execute(externalProjectPath, null) { connection ->
            connection.action()
            val gradleModel = connection.model(org.gradle.tooling.model.GradleProject::class.java).get()
            gradleModel.tasks
        }
        val buildTask = gradleTasks.firstOrNull { it.name == "build" }
        val cleanTask = gradleTasks.firstOrNull { it.name == "clean" }
        ensureNotNull(cleanTask) { CompilationPropertyCheckerError.NoBuildSchema }
        ensureNotNull(buildTask) { CompilationPropertyCheckerError.NoBuildSchema }
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

        val configuration = RunManager
            .getInstance(project)
            .createConfiguration(
                "Gradle Test Project Compilation",
                GradleExternalTaskConfigurationType.getInstance().factory
            ).configuration as GradleRunConfiguration
        configuration.settings.apply {
            externalSystemIdString = GradleConstants.SYSTEM_ID.id
            taskNames = listOf(task.path)
            this.externalProjectPath = externalProjectPath
            isPassParentEnvs = true
            scriptParameters = "--quiet"
        }

        val executor = DefaultRunExecutor.getRunExecutorInstance()

        val executionEnvironment = ExecutionEnvironmentBuilder
            .create(executor, configuration)
            .target(ExecutionTargetManager.getActiveTarget(project))
            .build()

        val runner = ProgramRunner.getRunner(executor.id, configuration)
        ensureNotNull(runner) { CompilationPropertyCheckerError.NoBuildSchema }

        executionEnvironment.setCallback { descriptor ->
            descriptor
                .processHandler
                ?.addProcessListener(processAdapter)
        }
        writeAction { runner.execute(executionEnvironment) }
        processAdapter.getRunResult()
    }
}