package org.plan.research.minimization.plugin.execution.gradle

import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.util.Key
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * An implementation of [com.intellij.execution.process.ProcessAdapter] that collects all the output
 * and passes it asynchronously to another object.
 */
class GradleRunProcessAdapter(
    private val cs: CoroutineScope
) : ProcessAdapter() {
    private val resultPipe: Channel<GradleConsoleRunResult> = Channel(capacity = 1)
    private val bufferStdOut = StringBuffer()
    private val bufferStdErr = StringBuffer()
    private val bufferSystem = StringBuffer()
    override fun processTerminated(event: ProcessEvent) {
        cs.launch {
            resultPipe.send(
                GradleConsoleRunResult(
                    event.exitCode,
                    bufferStdOut.toString(),
                    bufferStdErr.toString(),
                    bufferSystem.toString(),
                )
            )
            resultPipe.close()
        }
    }

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        when (outputType) {
            ProcessOutputType.STDERR -> bufferStdErr.append(event.text)
            ProcessOutputType.SYSTEM -> bufferSystem.append(event.text)
            ProcessOutputType.STDOUT -> bufferStdOut.append(event.text)
        }
    }

    /**
     * Suspends the current coroutine and waits to receive the result from the result pipe.
     * Some output (that could be send after [processTerminated]) will be trunk
     */
    suspend fun getRunResult() = resultPipe.receive()
}