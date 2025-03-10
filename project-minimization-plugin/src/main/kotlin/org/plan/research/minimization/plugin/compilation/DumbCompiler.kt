package org.plan.research.minimization.plugin.compilation

import org.plan.research.minimization.plugin.compilation.exception.CompilationException
import org.plan.research.minimization.plugin.compilation.transformer.ExceptionTransformer
import org.plan.research.minimization.plugin.context.IJDDContext

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
            transformation: ExceptionTransformer,
            context: IJDDContext,
        ): CompilationException = this
    }
}
