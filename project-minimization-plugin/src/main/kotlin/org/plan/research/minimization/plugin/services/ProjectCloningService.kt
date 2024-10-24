package org.plan.research.minimization.plugin.services

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.jetbrains.kotlin.idea.configuration.GRADLE_SYSTEM_ID
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings
import java.nio.file.Path
import java.util.*
import kotlin.io.path.copyTo
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo

/**
 * Service responsible for cloning a given project.
 *
 * @param rootProject The root project used as a source of services
 */
@Service(Service.Level.PROJECT)
class ProjectCloningService(private val rootProject: Project) {
    private val tempProjectsDirectoryName = rootProject
        .service<MinimizationPluginSettings>()
        .state
        .temporaryProjectLocation
        ?: ""
    private val importantFiles = setOf("modules.xml", "misc.xml", "libraries")

    private val logger = KotlinLogging.logger {}

    // TODO: JBRes-1977
    var isTest: Boolean = false

    suspend fun openGradleProject(projectPath: Path, import: Boolean): Project? {
        val project = ProjectUtil.openOrImportAsync(projectPath, OpenProjectTask {
            forceOpenInNewFrame = true
            runConversionBeforeOpen = false
            isRefreshVfsNeeded = !isTest
        }) ?: return null
        if (import) {
            forceImportGradleProject(project)
        }
        return project
    }

    private suspend fun forceImportGradleProject(project: Project) {
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

    suspend fun cloneProject(projectDir: VirtualFile): VirtualFile? {
        val clonedProjectPath = cloneProjectImpl(projectDir)
        return LocalFileSystem.getInstance().refreshAndFindFileByNioFile(clonedProjectPath)
    }

    /**
     * Perform a full clone of the project
     *
     * @param project A project to clone
     * @return a cloned project
     */
    suspend fun cloneAndOpenProject(project: Project): Project? {
        val projectDir = project.guessProjectDir() ?: return null

        val clonedProjectPath = cloneProjectImpl(projectDir)

        return openGradleProject(clonedProjectPath, false)
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

    private fun VirtualFile.copyTo(destination: Path, root: Boolean = true, filter: (VirtualFile) -> Boolean) {
        if (!filter(this)) {
            return
        }
        val originalPath = this.toNioPathOrNull() ?: return
        val fileDestination = if (root) destination else destination.resolve(name)
        try {
            originalPath.copyTo(fileDestination, overwrite = true)
        } catch (e: Throwable) {
            logger.error(e) { "Failed to copy file from $originalPath to $fileDestination" }
            return
        }
        if (isDirectory) {
            for (child in children) {
                child.copyTo(fileDestination, false, filter)
            }
        }
    }
}
