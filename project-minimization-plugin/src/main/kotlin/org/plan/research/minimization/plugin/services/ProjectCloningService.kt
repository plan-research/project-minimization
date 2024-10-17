package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.project.isProjectOrWorkspaceFile
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findOrCreateDirectory

import java.util.*

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

    /**
     * Perform a full clone of the project
     *
     * @param project A project to clone
     * @return a cloned project
     */
    suspend fun clone(project: Project): Project? {
        val projectRoot = project.guessProjectDir() ?: return null
        val clonedProjectPath = writeAction {
            val clonedProjectPath = createNewProjectDirectory()
            val snapshotLocation = getSnapshotLocation()
            VfsUtil.copyDirectory(
                this,
                projectRoot,
                clonedProjectPath,
            ) { it.path != snapshotLocation.path && isImportant(it, projectRoot) }
            clonedProjectPath
        }
        return ProjectUtil.openOrImportAsync(clonedProjectPath.toNioPath())
    }

    private fun isImportant(file: VirtualFile, root: VirtualFile): Boolean {
        val path = file.toNioPath().relativeTo(root.toNioPath())
        if (isProjectOrWorkspaceFile(file)) {
            val pathString = path.pathString
            return importantFiles.any { it in pathString }
        }
        return true
    }

    private fun createNewProjectDirectory(): VirtualFile =
        getSnapshotLocation().findOrCreateDirectory(UUID.randomUUID().toString())

    private fun getSnapshotLocation(): VirtualFile =
        rootProject
            .guessProjectDir()
            ?.findOrCreateDirectory(tempProjectsDirectoryName)
            ?: rootProject.guessProjectDir()!!
}
