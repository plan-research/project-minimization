package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.model.IJDDContext

import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.isProjectOrWorkspaceFile
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.api.errors.NoHeadException

import java.io.File
import java.util.*

import kotlin.io.path.pathString
import kotlin.io.path.relativeTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Service(Service.Level.APP)
class GitWrapperService {
    private val importantFiles = setOf("modules.xml", "misc.xml", "libraries")

    suspend fun commitChanges(context: IJDDContext): IJDDContext = context.apply {
        commit(this.projectDir)
        projectDir.refresh(false, true)
    }

    suspend fun resetChanges(context: IJDDContext) {
        withContext(Dispatchers.IO) {
            // git reset --hard
            Git.open(context.projectDir.toNioPath().toFile()).apply {
                reset().setMode(ResetCommand.ResetType.HARD).call()
                clean().setCleanDirectories(true).call()
                close()
            }
        }
    }

    private suspend fun commit(projectDir: VirtualFile) = withContext(Dispatchers.IO) {
        Git.open(projectDir.toNioPath().toFile()).let {
            projectDir.gitAdd(it) { file ->
                isImportant(file, projectDir)
            }
            it.commit().setMessage(UUID.randomUUID().toString()).setAllowEmpty(true)
                .call()
            it.close()
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

    fun gitInitOrOpen(VFProjectDir: VirtualFile): Git {
        val projectDir: File = VFProjectDir.toNioPath().toFile()
        if (projectDir.resolve(".git").exists()) {
            return Git.open(projectDir)
        }
        return Git.init()
            .setDirectory(projectDir)
            .call()
            .apply {
                commit().setMessage("init commit").call()
            }
    }

    private fun isCommitListEmpty(git: Git): Boolean = try {
        git.log().call()  // This will throw NoHeadException if there are no commits
        false
    } catch (e: NoHeadException) {
        true
    } finally {
        git.close()
    }

    /* returns all commits in the chronically reversed order */
    /* getCommitList(git)[0] == HEAD */
    public fun getCommitList(git: Git): List<String> {
        if (commitListEmpty(git)) {
            return listOf()
        }
        return git.log()
            .all()
            .call()
            .map { it.name }
    }

    private suspend fun VirtualFile.gitAdd(git: Git, filter: (VirtualFile) -> Boolean) {
        if (!filter(this)) {
            return
        }
        val originalPath = this.toNioPathOrNull() ?: return
        val projectDir = git.repository.directory.parentFile
        if (originalPath.toString().contains(".git")) {
            // merge with the filter
            return
        }
        try {
            withContext(Dispatchers.IO) {
                if (!FileUtil.filesEqual(originalPath.toFile(), projectDir)) {
                    git.add().addFilepattern(originalPath.toFile().relativeTo(projectDir).toString()).call()
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
