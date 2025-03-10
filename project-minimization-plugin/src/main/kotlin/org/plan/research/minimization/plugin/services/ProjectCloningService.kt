package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.context.HeavyIJDDContext
import org.plan.research.minimization.plugin.context.IJDDContextBase
import org.plan.research.minimization.plugin.context.IJDDContextCloner
import org.plan.research.minimization.plugin.context.LightIJDDContext
import org.plan.research.minimization.plugin.util.getCurrentTimeString

import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.io.findOrCreateDirectory
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDirectory
import com.intellij.openapi.vfs.toNioPathOrNull

import java.nio.file.Path
import java.util.*

import kotlin.io.path.Path
import kotlin.io.path.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service responsible for cloning a given project.
 *
 * @param rootProject The root project used as a source of services
 */
@Service(Service.Level.PROJECT)
class ProjectCloningService(private val rootProject: Project) : IJDDContextCloner {
    private val openingService = service<ProjectOpeningService>()
    private val state = rootProject
        .service<MinimizationPluginSettings>()
        .stateObservable
    private val logsDirectoryName by state
        .logsLocation
        .observe { it }
    private val tempProjectsDirectoryPath by state
        .temporaryProjectLocation
        .observe { it }

    override suspend fun <C : LightIJDDContext<C>> cloneLight(context: C): C? {
        val clonedPath = cloneProjectImpl(context.projectDir)
        val clonedProjectDir = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(clonedPath) ?: return null
        clonedProjectDir.refresh(false, true)
        return context.copy(projectDir = clonedProjectDir)
    }

    override suspend fun <C : HeavyIJDDContext<C>> cloneHeavy(context: C): C? {
        val clonedPath = cloneProjectImpl(context.projectDir)
        val clonedProject = openingService.openProject(clonedPath) ?: return null
        return context.copy(project = clonedProject)
    }

    private suspend fun cloneProjectImpl(projectDir: VirtualFile): Path {
        projectDir.refresh(false, true)
        return withContext(Dispatchers.IO) {
            val clonedProjectPath = createNewProjectDirectory()
            val logsLocation = getLogsLocation()
            projectDir.copyTo(clonedProjectPath) {
                isImportant(it, projectDir) && it.toNioPath() != logsLocation
            }
            clonedProjectPath
        }
    }

    private fun createNewProjectDirectory(): Path =
        Path(tempProjectsDirectoryPath)
            .findOrCreateDirectory("snapshot-${getCurrentTimeString()}-${UUID.randomUUID()}")

    private fun getLogsLocation(): Path? =
        rootProject
            .guessProjectDir()!!
            .findDirectory(logsDirectoryName)
            ?.toNioPath()

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

    suspend fun <C : IJDDContextBase<C>> clone(context: C): C? =
        context.clone(this)

    companion object {
        fun isImportant(file: VirtualFile, root: VirtualFile): Boolean =
            // JBRes-2481: make sure on each clone we re-import the project
            file.name != Project.DIRECTORY_STORE_FOLDER
    }
}
