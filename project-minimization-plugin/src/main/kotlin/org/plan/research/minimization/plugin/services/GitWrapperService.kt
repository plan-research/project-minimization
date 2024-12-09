package org.plan.research.minimization.plugin.services

import  org.plan.research.minimization.plugin.model.IJDDContext

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

/**
 * Service responsible for git operations within ProjectGitSnapshotManager.
 */
@Service(Service.Level.APP)
class GitWrapperService {
    private val importantFiles = setOf("modules.xml", "misc.xml", "libraries")

    suspend fun commitChanges(context: IJDDContext): IJDDContext = context.apply {
        commit(context)
        projectDir.refresh(false, true)
    }

    suspend fun resetChanges(context: IJDDContext) {
        withContext(Dispatchers.IO) {
            // git reset --hard
            context.git.apply {
                reset().setMode(ResetCommand.ResetType.HARD).call()
                clean().setCleanDirectories(true).call()
                close()
            }
        }
        context.projectDir.refresh(false, true)
    }

    private suspend fun commit(context: IJDDContext) = withContext(Dispatchers.IO) {
        context.git.apply {
            // git commit -a
            commit().setMessage(UUID.randomUUID().toString()).setAll(true).setAllowEmpty(true).call()
            close()
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

    suspend fun gitInit(virtualProjectDir: VirtualFile): Git {
        val projectDir: File = virtualProjectDir.toNioPath().toFile()

        if (projectDir.resolve(".git").exists()) {
            projectDir.resolve(".git").deleteRecursively()
        }
        if (projectDir.resolve(".gitignore").exists()) {
            projectDir.resolve(".gitignore").delete()
        }

        return Git.init()
            .setDirectory(projectDir)
            .call()
            .also {
                virtualProjectDir.gitAdd(it)  { file ->
                    isImportant(file, virtualProjectDir)
                }
                it.commit().setMessage("init commit").call()
                virtualProjectDir.refresh(false, true)
            }
    }

    private fun isCommitListEmpty(git: Git): Boolean = try {
        git.log().setMaxCount(1).call()  // This will throw NoHeadException if there are no commits
        false
    } catch (e: NoHeadException) {
        true
    } finally {
        git.close()
    }

    /* returns all commits in the chronically reversed order */
    /* getCommitList(git)[0] == HEAD */
    public fun getCommitList(git: Git): List<String> {
        if (isCommitListEmpty(git)) {
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
        if (originalPath.toString().contains(git.repository.directory.toString())) {
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
