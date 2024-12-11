package org.plan.research.minimization.plugin.execution.exception

import org.plan.research.minimization.plugin.model.CaretPosition
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.exception.CompilationException
import org.plan.research.minimization.plugin.model.exception.ExceptionTransformation

import kotlinx.serialization.Serializable

@Serializable
sealed interface KotlincException : CompilationException {
    override suspend fun apply(
        transformation: ExceptionTransformation,
        context: IJDDContext,
    ): KotlincException

    /**
     * A data class that represents `CompilationException` from Kotlin compiler.
     * For that file we can acquire three properties:
     * @property stacktrace a stacktrace from kotlin compiler
     * @property position a position where the exception occurred
     * @property additionalMessage a supplement human-readable message (including problematic IR Element) for debugging
     */
    @Serializable
    data class BackendCompilerException(
        val stacktrace: String,
        val position: CaretPosition,
        val additionalMessage: String? = null,
    ) : KotlincException {
        override suspend fun apply(
            transformation: ExceptionTransformation,
            context: IJDDContext,
        ) = transformation.transform(this, context)
    }

    /**
     * Represents a generic internal compiler exception that occurs within the Kotlin compiler.
     * It could be a frontend / backend (linker, etc.) / tooling exception that doesn't have `CompilerException` type
     *
     * @property stacktrace A string representing the stacktrace of the exception.
     * @property message A string representing a human-readable message describing the exception.
     */
    @Serializable
    data class GenericInternalCompilerException(
        val stacktrace: String?,
        val message: String,
    ) : KotlincException {
        override suspend fun apply(
            transformation: ExceptionTransformation,
            context: IJDDContext,
        ) = transformation.transform(this, context)
    }

    /**
     * A data class that represents a general compilation error from kotlin compiler e.g., syntax errors or just expected exceptions
     * @property position a position where the exception occurred
     * @property message a human-readable description of the problem (without position and severity)
     * @property severity a severity level
     */
    @Serializable
    data class GeneralKotlincException(
        val position: CaretPosition?,
        val message: String,
        val severity: KotlincErrorSeverity = KotlincErrorSeverity.UNKNOWN,
    ) : KotlincException {
        override suspend fun apply(
            transformation: ExceptionTransformation,
            context: IJDDContext,
        ) = transformation.transform(this, context)
    }
    @Serializable
    data class KspException(
        val message: String,
        val stacktrace: String,
        val severity: KotlincErrorSeverity = KotlincErrorSeverity.UNKNOWN,
    ) : KotlincException {
        override suspend fun apply(
            transformation: ExceptionTransformation,
            context: IJDDContext,
        ) = transformation.transform(this, context)
    }
}
