package org.plan.research.minimization.plugin.logging

import arrow.atomic.Atomic
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.sift.AbstractDiscriminator
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.div

class ExecutionDiscriminator : AbstractDiscriminator<ILoggingEvent>() {
    override fun getDiscriminatingValue(e: ILoggingEvent): String = loggingFolder.get()

    override fun getKey(): String = "executionLogDir"

    companion object {
        val loggingFolder = Atomic<String>(System.getProperty("idea.log.path"))

        inline fun <T> withLoggingFolder(baseFolder: Path, executionId: String, block: () -> T): T {
            val resultFolder = baseFolder.div(executionId).absolutePathString()
            val old = loggingFolder.getAndSet(resultFolder)
            try {
                return block()
            } finally {
                loggingFolder.set(old)
            }
        }
    }
}
