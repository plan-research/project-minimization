package org.plan.research.minimization.plugin.execution.comparable

import org.plan.research.minimization.plugin.model.exception.CompilationException
import org.plan.research.minimization.plugin.model.exception.ExceptionComparator

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
    private val loggingLocation = Path(System.getProperty("idea.log.path"), "exception-comparing")

    init {
        if (!loggingLocation.exists()) {
            loggingLocation.createDirectories()
        }
    }

    override fun areEquals(
        exception1: CompilationException,
        exception2: CompilationException,
    ): Boolean {
        if (!logger.isDebugEnabled) {
            return backedComparator.areEquals(exception1, exception2)
        }
        val newId = UUID.randomUUID().toString()
        logger.debug { "Comparing exceptions with id $newId" }
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
    this is LoggingExceptionComparator || !logger.isDebugEnabled -> this
    else -> LoggingExceptionComparator(this)
}
