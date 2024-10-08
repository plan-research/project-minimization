package org.plan.research.minimization.plugin.execution.gradle

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.intellij.execution.ExecutionTargetManager
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.plan.research.minimization.plugin.errors.CompilationPropertyCheckerError
import org.plan.research.minimization.plugin.model.CompilationPropertyChecker

class GradleCompilationPropertyChecker(private val cs: CoroutineScope) : CompilationPropertyChecker {
    override suspend fun checkCompilation(project: Project): Either<CompilationPropertyCheckerError, Throwable> {
        // FIXME: Check if project is using gradle
        val connector = Channel<GradleRunResult>()
        val runManager = RunManager.getInstance(project)
        val gradleConfigurationType = GradleExternalTaskConfigurationType.getInstance()
        val configuration = runManager.createConfiguration(
            "Gradle Test Project Compilation",
            gradleConfigurationType.factory
        ).configuration as GradleRunConfiguration
        configuration.settings.apply {
            taskNames = listOf("compileKotlin")
            externalProjectPath = project.basePath
        }

        val executor = DefaultRunExecutor.getRunExecutorInstance()

        val executionTarget = ExecutionTargetManager.getActiveTarget(project)
        val environmentBuilder = ExecutionEnvironmentBuilder.create(executor, configuration)
        val executionEnvironment = environmentBuilder.target(executionTarget).build()

        val runner = ProgramRunner.getRunner(executor.id, configuration) ?: TODO()

        executionEnvironment.setCallback { descriptor ->
            descriptor.processHandler?.addProcessListener(GradleRunProcessAdapter(connector))
        }
        runner.execute(executionEnvironment)

        return either {
            val result = runBlockingCancellable {
                connector.receive()
            }
            ensure(result.exitCode != 0) { CompilationPropertyCheckerError.CompilationSuccess }
            Throwable(message = result.output)
        }
    }

    private data class GradleRunResult(
        val exitCode: Int,
        val output: String
    )

    private inner class GradleRunProcessAdapter(val resultPipe: Channel<GradleRunResult>) : ProcessAdapter() {
        private val buffer = StringBuilder()
        override fun processTerminated(event: ProcessEvent) {
            cs.launch {
                resultPipe.send(GradleRunResult(event.exitCode, buffer.toString()))
                resultPipe.close()
            }
        }

        override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
            buffer.append(event.text)
        }
    }
}