package org.plan.research.minimization.plugin.execution.exception

import arrow.core.Either
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.raise.option
import com.intellij.build.events.BuildEvent
import com.intellij.build.events.FileMessageEvent
import com.intellij.build.events.MessageEvent
import org.jetbrains.kotlin.utils.addToStdlib.indexOfOrNull
import org.plan.research.minimization.plugin.errors.KotlincExceptionError
import org.plan.research.minimization.plugin.fromFilePosition
import org.plan.research.minimization.plugin.fromString
import org.plan.research.minimization.plugin.model.CaretPosition

object KotlincExceptionUtils {
    fun parseException(buildEvent: BuildEvent): Either<KotlincExceptionError, KotlincException> =
        either {
            if (buildEvent.isInternal()) {
                parseInternalException(buildEvent)
            } else {
                transformCompilationError(buildEvent)
            }.bind()
        }

    private fun transformCompilationError(buildEvent: BuildEvent) = either {
        ensure(buildEvent is FileMessageEvent) {
            raise(
                KotlincExceptionError.FailedToBuildException(
                    cause = IllegalStateException(
                        "Invalid event type: ${buildEvent.javaClass}, but expected FileMessageEvent"
                    )
                )
            )
        }
        val filePosition = CaretPosition.fromFilePosition(buildEvent.filePosition)
        KotlincException.GeneralKotlincException(filePosition, buildEvent.message, buildEvent.severity())
    }

    private fun parseInternalException(
        buildEvent: BuildEvent
    ): Either<KotlincExceptionError, KotlincException> = either {
        val (exceptionType, exceptionMessage) = parseExceptionMessage(buildEvent.description ?: "")
            ?: raise(KotlincExceptionError.NoExceptionFound)
        when (exceptionType) {
            BACKEND_COMPILER_EXCEPTION_CLASSNAME -> parseBackendCompilerException(exceptionMessage).bind()
            else -> parseGenericInternalCompilerException(exceptionMessage).bind()
        }
    }

    private fun parseBackendCompilerException(
        exceptionMessage: String
    ): Either<KotlincExceptionError, KotlincException.BackendCompilerException> = either {
        val (_, detailedMessage) = exceptionMessage.pollNextLine() // Remove boilerplate about making an issue
        ensureNotNull(detailedMessage) { KotlincExceptionError.NoExceptionFound }

        val (filePosition, remaining) = detailedMessage.pollNextLine()
        ensureNotNull(remaining) { KotlincExceptionError.NoExceptionFound }
        val (additionalMessage, stacktrace) = remaining.splitMessageAndStacktrace()
            .getOrElse { raise(KotlincExceptionError.NoExceptionFound) }

        KotlincException.BackendCompilerException(
            position = CaretPosition.fromString(filePosition),
            stacktrace = stacktrace,
            additionalMessage = additionalMessage,
        )
    }

    private fun parseGenericInternalCompilerException(
        exceptionMessage: String
    ): Either<KotlincExceptionError, KotlincException.GenericInternalCompilerException> = either {
        val (stacktrace, message) = exceptionMessage.splitMessageAndStacktrace()
            .getOrElse { raise(KotlincExceptionError.NoExceptionFound) }
        KotlincException.GenericInternalCompilerException(stacktrace, message)
    }


    /**
     * Checks whether the current BuildEvent instance is an internal Kotlin compiler exception.
     */
    private fun BuildEvent.isInternal(): Boolean =
        // FIXME: Not sure that is complete way to check if the exception is internal
        message.endsWith("Please report this problem https://kotl.in/issue")

    private const val COLON = ':'
    private const val BACKEND_COMPILER_EXCEPTION_CLASSNAME = "org.jetbrains.kotlin.backend.common.CompilationException"

    private fun parseExceptionMessage(message: String): Pair<String, String>? {
        val colonIndex = message.indexOfOrNull(COLON) ?: return null
        val exceptionType = message.substring(0, colonIndex)
        val exceptionMessage = message.substring(colonIndex + 1)
        return exceptionType to exceptionMessage.trim()
    }

    private fun String.pollNextLine(): Pair<String, String?> {
        val nextLineIndex = indexOf(System.lineSeparator()).takeIf { it >= 0 } ?: return this to null
        return substring(0, nextLineIndex) to substring(nextLineIndex + System.lineSeparator().length)
    }

    private fun String.splitMessageAndStacktrace(): Option<Pair<String, String>> = option {
        val lastLines = lines()
        val firstStacktraceLine = lastLines.indexOfFirst { it.startsWith("\tat") }
        ensure(firstStacktraceLine >= 0)
        lastLines.take(firstStacktraceLine).joinToString(separator = System.lineSeparator()) to
                lastLines.drop(firstStacktraceLine).joinToString(separator = System.lineSeparator())
    }
    private fun MessageEvent.severity() = when (this.kind) {
        MessageEvent.Kind.ERROR -> KotlincErrorSeverity.ERROR
        MessageEvent.Kind.WARNING -> KotlincErrorSeverity.WARNING
        MessageEvent.Kind.INFO -> KotlincErrorSeverity.INFO
        else -> KotlincErrorSeverity.UNKNOWN
    }
}