package org.plan.research.minimization.plugin.execution

import org.plan.research.minimization.plugin.errors.CompilationPropertyCheckerError
import org.plan.research.minimization.plugin.execution.DumbCompiler.targetPaths
import org.plan.research.minimization.plugin.model.BuildExceptionProvider
import org.plan.research.minimization.plugin.model.CompilationResult
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.exception.CompilationException
import org.plan.research.minimization.plugin.model.exception.ExceptionTransformation

import arrow.core.raise.either
import arrow.core.raise.ensureNotNull

/**
 * A dumb compiler that checks containing of [targetPaths].
 *
 * If [targetPaths] equals null, then [DumbCompiler] always returns new exception
 */
object DumbCompiler : BuildExceptionProvider {
    var targetPaths: List<String>? = null
    private val THROWABLE = Throwable()

    override suspend fun checkCompilation(context: IJDDContext): CompilationResult =
        either {
            val paths = targetPaths ?: return@either DumbException(Throwable())

            val baseDir = context.projectDir

            for (path in paths) {
                ensureNotNull(baseDir.findFileByRelativePath(path)) { CompilationPropertyCheckerError.CompilationSuccess }
            }

            DumbException(THROWABLE)
        }

    data class DumbException(val throwable: Throwable) : CompilationException {
        override suspend fun apply(
            transformation: ExceptionTransformation,
            context: IJDDContext,
        ): CompilationException = this
    }
}
