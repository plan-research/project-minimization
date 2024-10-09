package org.plan.research.minimization.plugin.execution.gradle

import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.util.Key
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch


class GradleRunProcessAdapter(
    private val cs: CoroutineScope
) : ProcessAdapter() {
    private val resultPipe: Channel<GradleRunResult> = Channel(capacity = 1)
    private val buffer = StringBuffer()
    override fun processTerminated(event: ProcessEvent) {
        cs.launch {
            resultPipe.send(GradleRunResult(event.exitCode, buffer.toString()))
            resultPipe.close()
        }
    }

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        buffer.append(event.text)
    }

    suspend fun getRunResult() = resultPipe.receive()
}