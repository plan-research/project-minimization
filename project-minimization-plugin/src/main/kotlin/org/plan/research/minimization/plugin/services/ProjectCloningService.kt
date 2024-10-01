package org.plan.research.minimization.plugin.services

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findOrCreateDirectory
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings
import java.util.*

/**
 * Service responsible for cloning a given project.
 *
 * @property rootProject The root project used as a source of services
 */
@Service(Service.Level.PROJECT)
class ProjectCloningService(private val rootProject: Project) {
    private val tempProjectsDirectoryName = rootProject
        .service<MinimizationPluginSettings>()
        .state
        .temporaryProjectLocation
        ?: ""

    /**
     * Perform a full clone of the project
     * @param project A project to clone
     * @return a cloned project or null if the project was default
     */
    suspend fun clone(project: Project): Project? {
        val clonedProjectPath = createNewProjectDirectory()
        val projectRoot = project.guessProjectDir() ?: return null
        writeAction {
            val snapshotLocation = getSnapshotLocation()
            VfsUtil.copyDirectory(
                this,
                projectRoot,
                clonedProjectPath
            ) { it.path != snapshotLocation.path }
        }
        return ProjectUtil.openOrImportAsync(clonedProjectPath.toNioPath())
    }

    @Suppress("UnstableApiUsage")
    private suspend fun createNewProjectDirectory(): VirtualFile = writeAction {
        getSnapshotLocation().findOrCreateDirectory(UUID.randomUUID().toString())
    }

    private fun getSnapshotLocation(): VirtualFile =
        rootProject
            .guessProjectDir()
            ?.findOrCreateDirectory(tempProjectsDirectoryName)
            ?: rootProject.guessProjectDir()!!
}
