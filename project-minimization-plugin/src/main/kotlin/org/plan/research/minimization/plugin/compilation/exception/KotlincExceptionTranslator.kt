package org.plan.research.minimization.plugin.compilation.exception

import org.plan.research.minimization.plugin.compilation.CompilationPropertyCheckerError

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

typealias ParseKotlincExceptionResult = Either<CompilationPropertyCheckerError, KotlincException>

/**
 * KotlincExceptionTranslator provides functionality to interpret build events and transform them into either
 * general compilation errors or internal Kotlin compiler exceptions.
 */
class KotlincExceptionTranslator {
    /**
     * Parses a [com.intellij.build.events.BuildEvent] to determine if it represents an internal or general Kotlin compiler exception.
     *
     * @param event The [com.intellij.build.events.BuildEvent] instance to parse.
     * @return An [Either] containing a [CompilationPropertyCheckerError] if parsing fails,
     * or a [KotlincException] representing the parsed exception.
     */
    fun parseException(event: BuildEvent): ParseKotlincExceptionResult =
        either {
            if (event.isInternal()) {
                parseInternalException(event)
            } else {
                transformCompilationError(event)
            }.bind()
        }

    private fun transformCompilationError(buildEvent: BuildEvent) = either {
        if (buildEvent.message.startsWith("[ksp]")) {
            return@either parseKspError(buildEvent).bind()
        }
        ensure(buildEvent is MessageEvent) {
            raise(
                CompilationPropertyCheckerError.BuildSystemFail(
                    cause = IllegalStateException(
                        "Invalid event type: ${buildEvent.javaClass}, but expected MessageEvent",
                    ),
                ),
            )
        }
        val filePosition = if (buildEvent is FileMessageEvent) {
            CaretPosition.fromFilePosition(buildEvent.filePosition)
        } else {
            null
        }
        KotlincException.GeneralKotlincException(filePosition, buildEvent.message, buildEvent.severity())
    }

    private fun parseKspError(event: BuildEvent) = either {
        val messageWithoutKsp = event.message.removePrefix("[ksp] ")
        val (additionalMessage, stacktrace) = messageWithoutKsp.splitMessageAndStacktrace()
            .getOrElse { raise(CompilationPropertyCheckerError.CompilationSuccess) }
        KotlincException.KspException(
            additionalMessage,
            stacktrace,
            (event as? MessageEvent)?.severity() ?: KotlincErrorSeverity.UNKNOWN,
        )
    }

    private fun parseInternalException(
        buildEvent: BuildEvent,
    ): ParseKotlincExceptionResult = either {
        val (exceptionType, exceptionMessage) = parseExceptionMessage(buildEvent.description ?: "")
            ?: raise(CompilationPropertyCheckerError.CompilationSuccess)
        when (exceptionType) {
            BACKEND_COMPILER_EXCEPTION_CLASSNAME, COMMON_BACKEND_EXCEPTION_CLASSNAME ->
                parseBackendCompilerException(exceptionMessage).bind()

            else -> parseGenericInternalCompilerException(exceptionMessage).bind()
        }
    }

    private fun parseBackendCompilerException(
        exceptionMessage: String,
    ): ParseKotlincExceptionResult = either {
        val (_, detailedMessage) = exceptionMessage.pollNextLine()  // Remove boilerplate about making an issue
        ensureNotNull(detailedMessage) { CompilationPropertyCheckerError.CompilationSuccess }

        val (filePosition, remaining) = detailedMessage.pollNextLine()
        ensureNotNull(remaining) { CompilationPropertyCheckerError.CompilationSuccess }
        val (additionalMessage, stacktrace) = remaining.splitMessageAndStacktrace()
            .getOrElse { raise(CompilationPropertyCheckerError.CompilationSuccess) }

        KotlincException.BackendCompilerException(
            position = CaretPosition.fromString(filePosition),
            stacktrace = stacktrace,
            additionalMessage = additionalMessage,
        )
    }

    private fun parseGenericInternalCompilerException(
        exceptionMessage: String,
    ): Either<CompilationPropertyCheckerError, KotlincException.GenericInternalCompilerException> = either {
        val (message, stacktrace) = exceptionMessage.splitMessageAndStacktrace()
            .getOrElse { return@either KotlincException.GenericInternalCompilerException(null, exceptionMessage) }
        KotlincException.GenericInternalCompilerException(stacktrace, message)
    }

    /**
     * Checks whether the current BuildEvent instance is an internal Kotlin compiler exception.
     */
    private fun BuildEvent.isInternal(): Boolean =
        // FIXME: Not sure that is complete way to check if the exception is internal
        message.endsWith("Please report this problem https://kotl.in/issue") ||
            message.startsWith("org.jetbrains.kotlin.util.FileAnalysisException") ||
            message.startsWith(BACKEND_COMPILER_EXCEPTION_CLASSNAME) ||
            message.startsWith(COMMON_BACKEND_EXCEPTION_CLASSNAME) ||
            message.startsWith(FILE_ANALYSIS_EXCEPTION_CLASSNAME)

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

    @Suppress("TYPE_ALIAS")
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

    companion object {
        private const val BACKEND_COMPILER_EXCEPTION_CLASSNAME =
            "org.jetbrains.kotlin.backend.common.CompilationException"
        private const val COLON = ':'

        private const val COMMON_BACKEND_EXCEPTION_CLASSNAME =
            "org.jetbrains.kotlin.backend.common.BackendException"  // old version, I suppose
        private const val FILE_ANALYSIS_EXCEPTION_CLASSNAME =
            "org.jetbrains.kotlin.util.FileAnalysisException"
    }
}
