package org.plan.research.minimization.plugin.logging

import org.plan.research.minimization.plugin.compilation.comparator.ExceptionComparator
import org.plan.research.minimization.plugin.compilation.exception.CompilationException

import mu.KotlinLogging

import java.util.UUID

import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger {}

class LoggingExceptionComparator(private val backedComparator: ExceptionComparator) : ExceptionComparator {
    private val logger = KotlinLogging.logger {}
    private val loggingLocation = Path(ExecutionDiscriminator.loggingFolder.get(), "exception-comparing")
    private var iteration = 0

    init {
        if (!loggingLocation.exists()) {
            loggingLocation.createDirectories()
        }
    }

    override suspend fun areEquals(
        exception1: CompilationException,
        exception2: CompilationException,
    ): Boolean {
        if (!logger.isTraceEnabled) {
            return backedComparator.areEquals(exception1, exception2)
        }
        val newId = "${++iteration}-${UUID.randomUUID()}"
        logger.trace { "Comparing exceptions with id $newId" }
        val serializableClass = ExceptionComparison(
            exception1.toString(),
            exception2.toString(),
            backedComparator.areEquals(exception1, exception2),
        )
        loggingLocation
            .resolve("$newId.json")
            .writeText(json.encodeToString(serializableClass))

        return serializableClass.result
    }

    @Serializable
    private data class ExceptionComparison(val exception1: String, val exception2: String, val result: Boolean)

    companion object {
        private val json = Json { prettyPrint = true }
    }
}

fun ExceptionComparator.withLogging(): ExceptionComparator = when {
    this is LoggingExceptionComparator || !logger.isTraceEnabled -> this
    else -> LoggingExceptionComparator(this)
}
