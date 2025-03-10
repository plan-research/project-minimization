package org.plan.research.minimization.plugin.compilation.transformer

import org.plan.research.minimization.plugin.compilation.exception.IdeaCompilationException
import org.plan.research.minimization.plugin.compilation.exception.KotlincException
import org.plan.research.minimization.plugin.context.IJDDContext

import com.intellij.openapi.vfs.toNioPathOrNull

import java.nio.file.Path

import kotlin.io.path.Path
import kotlin.io.path.relativeTo
import kotlin.text.get

/**
 * Transforms file paths in compiler exceptions to be relative to a project root.
 */
class PathRelativizationTransformer : ExceptionTransformer {
    override suspend fun transform(
        exception: IdeaCompilationException,
        context: IJDDContext,
    ) =
        exception.copy(kotlincExceptions = exception.kotlincExceptions.map {
            it.apply(
                this@PathRelativizationTransformer,
                context,
            )
        })

    override suspend fun transform(exception: KotlincException.GeneralKotlincException, context: IJDDContext): KotlincException.GeneralKotlincException {
        val copiedCursorPosition = exception.position?.let { position ->
            val transformedPath = transformPath(position.filePath, context)
            position.copy(filePath = transformedPath)
        }

        return exception.copy(position = copiedCursorPosition, message = exception.message.replaceRootDir(context))
    }

    override suspend fun transform(
        exception: KotlincException.GenericInternalCompilerException,
        context: IJDDContext,
    ): KotlincException.GenericInternalCompilerException = exception.copy(message = exception.message.replaceRootDir(context))

    override suspend fun transform(
        exception: KotlincException.BackendCompilerException,
        context: IJDDContext,
    ): KotlincException.BackendCompilerException {
        val transformedPath = transformPath(exception.position.filePath, context)
        val copiedCursorPosition = exception.position.copy(filePath = transformedPath)
        return exception.copy(
            position = copiedCursorPosition,
            additionalMessage = exception.additionalMessage?.replaceRootDir(context),
        )
    }

    override suspend fun transform(exception: KotlincException.KspException, context: IJDDContext): KotlincException.KspException =
        exception.copy(message = exception.message.replaceRootDir(context))

    private fun transformPath(path: Path, context: IJDDContext): Path {
        val projectBase = context.projectDir.toNioPathOrNull() ?: return path
        return path.relativeTo(projectBase)
    }

    /**
     * Tries to replace root paths in the string.
     * Due to the uncertain format with the path inside the messages we just try to find `<file>:<line>:<column>` entries
     */
    private fun String.replaceRootDir(context: IJDDContext): String = this
        .replace(FILE_ENTRY_REGEX) {
            val fileUrl = it.groups["fileurl"]?.value ?: ""
            val name = it.groups["name"]?.value!!
            val line = it.groups["line"]?.value
            val column = it.groups["column"]?.value
            val relativePath = transformPath(Path(name), context)
            "$fileUrl$relativePath:$line:$column"
        }

    companion object {
        private val FILE_ENTRY_REGEX = Regex("(?<fileurl>file://)?(?<name>/.+?):(?<line>-?\\d+):(?<column>-?\\d+)")
    }
}
