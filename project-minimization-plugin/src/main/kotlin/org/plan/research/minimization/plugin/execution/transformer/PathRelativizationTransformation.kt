package org.plan.research.minimization.plugin.execution.transformer

import org.plan.research.minimization.plugin.execution.IdeaCompilationException
import org.plan.research.minimization.plugin.execution.exception.KotlincException.*
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.exception.ExceptionTransformation
import org.plan.research.minimization.plugin.settings.MinimizationPluginStateObservable

import com.intellij.openapi.vfs.toNioPathOrNull

import java.nio.file.Path

import kotlin.io.path.Path
import kotlin.io.path.relativeTo

/**
 * Transforms file paths in compiler exceptions to be relative to a project root.
 * This module uses the fact that [MinimizationPluginState.temporaryProjectLocation][MinimizationPluginStateObservable.temporaryProjectLocation]
 * is used for storing temporary projects.
 */
class PathRelativizationTransformation : ExceptionTransformation {
    override suspend fun transform(
        exception: IdeaCompilationException,
        context: IJDDContext,
    ) =
        exception.copy(kotlincExceptions = exception.kotlincExceptions.map {
            it.apply(
                this@PathRelativizationTransformation,
                context,
            )
        })

    override suspend fun transform(exception: GeneralKotlincException, context: IJDDContext): GeneralKotlincException {
        val copiedCursorPosition = exception.position?.let { position ->
            val transformedPath = transformPath(position.filePath, context)
            position.copy(filePath = transformedPath)
        }

        return exception.copy(position = copiedCursorPosition, message = exception.message.replaceRootDir(context))
    }

    override suspend fun transform(
        exception: GenericInternalCompilerException,
        context: IJDDContext,
    ): GenericInternalCompilerException = exception.copy(message = exception.message.replaceRootDir(context))

    override suspend fun transform(
        exception: BackendCompilerException,
        context: IJDDContext,
    ): BackendCompilerException {
        val transformedPath = transformPath(exception.position.filePath, context)
        val copiedCursorPosition = exception.position.copy(filePath = transformedPath)
        return exception.copy(
            position = copiedCursorPosition,
            additionalMessage = exception.additionalMessage?.replaceRootDir(context),
        )
    }

    override suspend fun transform(exception: KspException, context: IJDDContext): KspException =
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
