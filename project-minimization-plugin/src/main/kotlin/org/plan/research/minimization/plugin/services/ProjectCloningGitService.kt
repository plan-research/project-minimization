package org.plan.research.minimization.plugin.services

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.plan.research.minimization.plugin.model.HeavyIJDDContext
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.LightIJDDContext
import java.nio.file.Path
import java.util.*
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo
import org.eclipse.jgit.api.Git
import java.io.File

@Service(Service.Level.PROJECT)
class ProjectCloningGitService(private val rootProject: Project) {
    private val openingService = service<ProjectOpeningService>()
    private val tempProjectsDirectoryName by rootProject
        .service<MinimizationPluginSettings>()
        .stateObservable
        .temporaryProjectLocation
        .observe { it }
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
            //TODO Git.init().setDirectory(projectDir.toNioPath().toFile()).call()
            Git.open(projectDir.toNioPath().toFile()).let {
                projectDir.gitAdd(it) { file ->
                    isImportant(file, projectDir)
                }
                it.commit().setMessage(UUID.randomUUID().toString()).call()
                it.close()
            }
            projectDir.toNioPath()
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

    private suspend fun VirtualFile.gitAdd(git: Git, filter: (VirtualFile) -> Boolean) {
        if (!filter(this)) {
            return
        }
        val originalPath = this.toNioPathOrNull() ?: return
        if (originalPath.toString().contains(".git")) {
            return
        }
        try {
            withContext(Dispatchers.IO) {
                git.add().addFilepattern(originalPath.pathString).call()
                //println(originalPath.pathString)
            }
        } catch (e: Throwable) {
            return
        }
        if (isDirectory) {
            val childrenCopy = readAction { children }
            for (child in childrenCopy) {
                child.gitAdd(git, filter)
            }
        }
    }
}