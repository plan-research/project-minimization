package org.plan.research.minimization.plugin.execution.transformer

import arrow.core.None
import arrow.core.Option
import arrow.core.raise.option
import arrow.core.toOption
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.util.io.isAncestor
import org.plan.research.minimization.plugin.drop
import org.plan.research.minimization.plugin.execution.IdeaCompilationException
import org.plan.research.minimization.plugin.execution.exception.KotlincException.*
import org.plan.research.minimization.plugin.model.exception.ExceptionTransformation
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.relativeTo

/**
 * Transforms file paths in compiler exceptions to be relative to a project root.
 * This module uses the fact that [MinimizationPluginState.temporaryProjectLocation][org.plan.research.minimization.plugin.settings.MinimizationPluginState.temporaryProjectLocation] is used for storing temporary projects.
 */
class PathRelativizationTransformation(rootProject: Project) : ExceptionTransformation {
    private val rootProjectBasePath = rootProject.guessProjectDir()!!.toNioPath()
    private val temporaryProjectsLocation = rootProjectBasePath
        .resolve(rootProject.service<MinimizationPluginSettings>().state.temporaryProjectLocation ?: "")

    override suspend fun transform(exception: IdeaCompilationException): Option<IdeaCompilationException> = option {
        exception.copy(kotlincExceptions = exception.kotlincExceptions.map { transform(it).bind() })
    }

    override suspend fun transform(exception: GeneralKotlincException): Option<GeneralKotlincException> = option {
        val transformedPath = transformPath(exception.position.filePath).bind()
        val copiedCursorPosition = exception.position.copy(filePath = transformedPath)
        exception.copy(position = copiedCursorPosition, message = exception.message.replaceRootDir())
    }
    override suspend fun transform(exception: GenericInternalCompilerException): Option<GenericInternalCompilerException> =
        exception.copy(message = exception.message.replaceRootDir()).toOption()
    override suspend fun transform(exception: BackendCompilerException): Option<BackendCompilerException> = option {
        val transformedPath = transformPath(exception.position.filePath).bind()
        val copiedCursorPosition = exception.position.copy(filePath = transformedPath)
        exception.copy(position = copiedCursorPosition, additionalMessage = exception.additionalMessage?.replaceRootDir())
    }

    private fun transformPath(path: Path): Option<Path> = option {
        when {
            temporaryProjectsLocation.isAncestor(path) -> path.relativeTo(temporaryProjectsLocation).drop(1)
            path.startsWith(rootProjectBasePath) -> path.relativeTo(rootProjectBasePath)
            else -> raise(None)
        }
    }

    /**
     * Tries to replace root paths in the string.
     * Due to the uncertain format with the path inside the messages we just try to find `<file>:<line>:<column>` entries
     */
    private fun String.replaceRootDir(): String = this
        .replace(FILE_ENTRY_REGEX) {
            val fileUrl = it.groups["fileurl"]?.value ?: ""
            val name = it.groups["name"]?.value!!
            val line = it.groups["line"]?.value
            val column = it.groups["column"]?.value
            val relativePath = transformPath(Path(name)).getOrNull() ?: name
            "$fileUrl$relativePath:$line:$column"
        }

    companion object {
        private val FILE_ENTRY_REGEX = Regex("(?<fileurl>file://)?(?<name>/.+?):(?<line>-?\\d+):(?<column>-?\\d+)")
    }
}