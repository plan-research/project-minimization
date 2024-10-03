package org.plan.research.minimization.plugin.execution

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import org.plan.research.minimization.plugin.errors.CompilationPropertyCheckerError
import org.plan.research.minimization.plugin.execution.DumbCompiler.targetPaths
import org.plan.research.minimization.plugin.model.CompilationPropertyChecker

/**
 * A dumb compiler that checks containing of [targetPaths].
 *
 * If [targetPaths] equals null, then [DumbCompiler] always returns new exception
 */
object DumbCompiler : CompilationPropertyChecker {
    override suspend fun checkCompilation(project: Project): Either<CompilationPropertyCheckerError, Throwable> =
        either {
            val paths = targetPaths ?: return@either Throwable()

            val baseDir = project.guessProjectDir() ?: raise(CompilationPropertyCheckerError.CompilationSuccess)

            for (path in paths) {
                ensureNotNull(baseDir.findFileByRelativePath(path)) { CompilationPropertyCheckerError.CompilationSuccess }
            }

            THROWABLE
        }

    var targetPaths: List<String>? = null

    private val THROWABLE = Throwable()
}