package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.project.isProjectOrWorkspaceFile
import com.intellij.openapi.util.io.findOrCreateDirectory
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import org.jetbrains.kotlin.idea.configuration.GRADLE_SYSTEM_ID

import java.nio.file.Path
import java.util.*

import kotlin.io.path.copyTo
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Service responsible for cloning a given project.
 *
 * @param rootProject The root project used as a source of services
 */
@Service(Service.Level.PROJECT)
class ProjectCloningService(private val rootProject: Project) {
    private val tempProjectsDirectoryName by rootProject
        .service<MinimizationPluginSettings>()
        .state
        .temporaryProjectLocation
        .onChange { it }
    private val importantFiles = setOf("modules.xml", "misc.xml", "libraries")

    // TODO: JBRes-1977
    var isTest: Boolean = false

    suspend fun forceImportGradleProject(project: Project) {
        if (!isTest) {
            try {
                ExternalSystemUtil.refreshProject(
                    project.guessProjectDir()!!.path,
                    ImportSpecBuilder(project, GRADLE_SYSTEM_ID)
                        .use(ProgressExecutionMode.MODAL_SYNC)
                        .build(),
                )
            } catch (e: Throwable) {
                withContext(NonCancellable) {
                    ProjectManagerEx.getInstanceEx().forceCloseProjectAsync(project)
                }
                throw e
            }
        }
    }

    /**
     * Perform a full clone of the project
     *
     * @param project A project to clone
     * @return a cloned project
     */
    suspend fun clone(project: Project): Project? {
        val projectRoot = project.guessProjectDir() ?: return null

        projectRoot.refresh(false, true)

        val clonedProjectPath = withContext(Dispatchers.IO) { createNewProjectDirectory() }
        val snapshotLocation = getSnapshotLocation()
        projectRoot.copyTo(clonedProjectPath) {
            isImportant(it, projectRoot) && it.toNioPath() != snapshotLocation
        }

        LocalFileSystem.getInstance().refreshAndFindFileByNioFile(clonedProjectPath)
            ?.refresh(false, true)

        return ProjectUtil.openOrImportAsync(clonedProjectPath, OpenProjectTask {
            forceOpenInNewFrame = true
            runConversionBeforeOpen = false
            isRefreshVfsNeeded = !isTest
        })
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
