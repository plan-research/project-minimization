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
import org.plan.research.minimization.plugin.getAllNestedElements
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings
import java.util.*

@Service(Service.Level.PROJECT)
class ProjectCloningService(private val rootProject: Project) {
    private val tempProjectsDirectoryName = rootProject
        .service<MinimizationPluginSettings>()
        .state
        .temporaryProjectLocation
        ?: ""

    suspend fun clone(project: Project): Project? {
        val projectRoot = project.guessProjectDir() ?: return null
        return clone(project, projectRoot.getAllNestedElements())
    }

    suspend fun clone(project: Project, items: List<VirtualFile>): Project? {
        val clonedProjectPath = createNewProjectDirectory()
        val projectRoot = project.guessProjectDir() ?: return null
        val snapshotLocation = getSnapshotLocation()
        writeAction {
            VfsUtil.copyDirectory(
                this,
                projectRoot,
                clonedProjectPath
            ) { it in items && it.path != snapshotLocation.path }
        }
        return ProjectUtil.openOrImportAsync(clonedProjectPath.toNioPath())
    }

    @Suppress("UnstableApiUsage")
    private suspend fun createNewProjectDirectory(): VirtualFile {
        val tempDirectory = getSnapshotLocation()
        return writeAction {
            tempDirectory
                .findOrCreateDirectory(UUID.randomUUID().toString())
        }
    }

    private suspend fun getSnapshotLocation(): VirtualFile = writeAction {
        rootProject
            .guessProjectDir()
            ?.findOrCreateDirectory(tempProjectsDirectoryName)
            ?: rootProject.guessProjectDir()!!
    }
}