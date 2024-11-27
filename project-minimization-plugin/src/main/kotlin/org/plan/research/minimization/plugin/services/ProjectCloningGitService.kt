package org.plan.research.minimization.plugin.services

import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.isProjectOrWorkspaceFile
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.platform.ijent.spi.connectToRunningIjent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.plan.research.minimization.plugin.model.IJDDContext
import java.util.*
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo
import org.eclipse.jgit.api.Git


@Service(Service.Level.PROJECT)
class ProjectCloningGitService(private val rootProject: Project? = null) {
    private val importantFiles = setOf("modules.xml", "misc.xml", "libraries")

    suspend fun commitChanges(context: IJDDContext): IJDDContext {
        return context.apply {
            commit(this.projectDir)
            projectDir.refresh(false, true)
        }
    }

    private suspend fun commit(projectDir: VirtualFile) {
        projectDir.refresh(false, true) // ???
        return withContext(Dispatchers.IO) {
            Git.open(projectDir.toNioPath().toFile()).let {
//                println("HEAD ${it.repository.resolve("HEAD")?.name}")
//                println(it.branchList().call()?.map { it.name })
                projectDir.gitAdd(it) { file ->
                    isImportant(file, projectDir)
                }
                it.commit().setMessage(UUID.randomUUID().toString()).setAllowEmpty(true).call()
//                println("HEAD ${it.repository.resolve("HEAD")?.name}")
//                println(it.branchList().call()?.map { it.name })
                it.close()
            }
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
        val projectDir = git.repository.directory.parentFile
        if (originalPath.toString().contains(".git")) { // merge with the filter
            return
        }
        try {
            withContext(Dispatchers.IO) {
                println(originalPath.toFile().relativeTo(projectDir))
                if (!FileUtil.filesEqual(originalPath.toFile(), projectDir)) {
                    git.add().addFilepattern(originalPath.toFile().relativeTo(projectDir).toString()).call()
                    println("added")
                }
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