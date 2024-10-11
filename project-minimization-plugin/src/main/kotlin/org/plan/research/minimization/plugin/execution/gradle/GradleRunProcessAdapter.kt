package org.plan.research.minimization.plugin.execution.gradle

import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.util.Key
import kotlinx.coroutines.channels.Channel


class GradleRunProcessAdapter : ProcessAdapter() {
    private val resultPipe: Channel<GradleConsoleRunResult> = Channel(capacity = 1)
    private val buffer = StringBuffer()
    override fun processTerminated(event: ProcessEvent) {
        resultPipe.trySend(GradleConsoleRunResult(event.exitCode, buffer.toString()))
        resultPipe.close()
    }

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        buffer.append(event.text)
    }

    suspend fun getRunResult() = resultPipe.receive()
}