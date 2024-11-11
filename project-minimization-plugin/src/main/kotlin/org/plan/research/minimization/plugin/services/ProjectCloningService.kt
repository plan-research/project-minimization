package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.model.HeavyIJDDContext
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.LightIJDDContext
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings

import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.project.isProjectOrWorkspaceFile
import com.intellij.openapi.util.io.findOrCreateDirectory
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull

import java.nio.file.Path
import java.util.*

import kotlin.io.path.copyTo
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service responsible for cloning a given project.
 *
 * @param rootProject The root project used as a source of services
 */
@Service(Service.Level.PROJECT)
class ProjectCloningService(private val rootProject: Project) {
    private val openingService = service<ProjectOpeningService>()
    private val tempProjectsDirectoryName = rootProject
        .service<MinimizationPluginSettings>()
        .state
        .temporaryProjectLocation
        ?: ""
    private val importantFiles = setOf("modules.xml", "misc.xml", "libraries")

    suspend fun clone(context: IJDDContext): IJDDContext? =
        when (context) {
            is HeavyIJDDContext -> clone(context)
            is LightIJDDContext -> clone(context)
        }

    suspend fun clone(context: LightIJDDContext): LightIJDDContext? {
        val clonedPath = cloneProjectImpl(context.projectDir)
        val clonedProjectDir = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(clonedPath) ?: return null
        clonedProjectDir.refresh(false, true)
        return context.copy(projectDir = clonedProjectDir)
    }

    suspend fun clone(context: HeavyIJDDContext): HeavyIJDDContext? {
        val clonedPath = cloneProjectImpl(context.projectDir)
        val clonedProject = openingService.openProject(clonedPath) ?: return null
        LocalFileSystem.getInstance().refreshAndFindFileByNioFile(clonedPath)?.refresh(false, true)
        return context.copy(project = clonedProject)
    }

    private suspend fun cloneProjectImpl(projectDir: VirtualFile): Path {
        projectDir.refresh(false, true)
        return withContext(Dispatchers.IO) {
            val clonedProjectPath = createNewProjectDirectory()
            val snapshotLocation = getSnapshotLocation()
            projectDir.copyTo(clonedProjectPath) {
                isImportant(it, projectDir) && it.toNioPath() != snapshotLocation
            }
            clonedProjectPath
        }
    }

    private fun isImportant(file: VirtualFile, root: VirtualFile): Boolean {
        val path = file.toNioPath().relativeTo(root.toNioPath())
        if (isProjectOrWorkspaceFile(file) && file.name != Project.DIRECTORY_STORE_FOLDER) {
            val pathString = path.pathString
            return importantFiles.any { it in pathString }
        }
        return true
    }

    private fun createNewProjectDirectory(): Path =
        getSnapshotLocation().findOrCreateDirectory(UUID.randomUUID().toString())

    private fun getSnapshotLocation(): Path =
        rootProject
            .guessProjectDir()!!
            .toNioPath()
            .findOrCreateDirectory(tempProjectsDirectoryName)

    private suspend fun VirtualFile.copyTo(destination: Path, root: Boolean = true, filter: (VirtualFile) -> Boolean) {
        if (!filter(this)) {
            return
        }
        val originalPath = this.toNioPathOrNull() ?: return
        val fileDestination = if (root) destination else destination.resolve(name)
        try {
            withContext(Dispatchers.IO) {
                originalPath.copyTo(fileDestination, overwrite = true)
            }
        } catch (e: Throwable) {
            return
        }
        if (isDirectory) {
            val childrenCopy = readAction { children }
            for (child in childrenCopy) {
                child.copyTo(fileDestination, false, filter)
            }
        }
    }
}
