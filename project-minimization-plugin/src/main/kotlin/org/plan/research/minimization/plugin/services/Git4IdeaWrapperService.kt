package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.model.context.IJDDContext

import com.intellij.history.LocalHistory
import com.intellij.history.LocalHistoryAction
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler

import java.io.File
import java.util.*

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service responsible for git4Idea operations within ProjectGitSnapshotManager.
 */
@Service(Service.Level.APP)
class Git4IdeaWrapperService {
    suspend fun commitChanges(context: IJDDContext) {
        withContext(Dispatchers.IO) {
            commit(context)
        }
    }

    suspend fun resetChanges(context: IJDDContext) {
        val project = context.indexProject
        withContext(Dispatchers.IO) {
            val git = Git.getInstance()
            val handler: GitLineHandler = GitLineHandler(project, context.indexProjectDir, GitCommand.RESET)
            handler.addParameters("--hard")

            git.runCommand(handler)
        }
        context.projectDir.refresh(false, true)
    }

    private suspend fun commit(context: IJDDContext) {
        val project = context.indexProject

        withContext(Dispatchers.IO) {
            val git = Git.getInstance()
            val handler: GitLineHandler = GitLineHandler(project, context.indexProjectDir, GitCommand.COMMIT)
            handler.addParameters("-m", UUID.randomUUID().toString(), "--allow-empty", "-a")

            val action = startLocalHistoryAction(project, "Save Project Snapshot")

            git.runCommand(handler)
        }
        context.projectDir.refresh(false, true)
    }

    suspend fun gitInit(context: IJDDContext, filter: (VirtualFile) -> Boolean) {
        val project = context.indexProject
        val virtualProjectDir = context.indexProjectDir

        withContext(Dispatchers.IO) {
            val git = Git.getInstance()
            val projectDir: File = virtualProjectDir.toNioPath().toFile()

            // Delete existing .git directory if present
            projectDir.resolve(".git").takeIf { it.exists() }?.deleteRecursively()
            projectDir.resolve(".gitignore").takeIf { it.exists() }?.delete()

            // Initialize Git repository
            git.init(project, virtualProjectDir).success().also {
                virtualProjectDir.gitAdd(context, project, filter)
                commit(context)
            }
            context.projectDir.refresh(false, true)
        }
    }

    fun getCommitList(context: IJDDContext): List<String> {
        val project = context.indexProject
        val git = Git.getInstance()
        val handler: GitLineHandler = GitLineHandler(project, context.indexProjectDir, GitCommand.LOG)
        handler.addParameters("--pretty=format:%H")

        val result = git.runCommand(handler)
        return if (result.success()) result.output else emptyList()
    }

    private suspend fun VirtualFile.gitAdd(context: IJDDContext, project: Project, filter: (VirtualFile) -> Boolean) {
        if (!filter(this)) {
            return
        }
        val originalPath = this.toNioPathOrNull() ?: return
        val projectDir: File = context.projectDir.toNioPathOrNull()!!.toFile()
        if (originalPath.toString().contains(context.projectDir.toString())) {
            return
        }
        try {
            withContext(Dispatchers.IO) {
                if (!FileUtil.filesEqual(originalPath.toFile(), projectDir)) {
                    val git = Git.getInstance()
                    val handler: GitLineHandler = GitLineHandler(project, context.indexProjectDir, GitCommand.ADD)
                    handler.addParameters(originalPath.toFile().relativeTo(projectDir).toString())

                    git.runCommand(handler)
                }
            }
        } catch (e: Throwable) {
            return
        }
        if (isDirectory) {
            val childrenCopy = readAction { children }
            for (child in childrenCopy) {
                child.gitAdd(context, project, filter)
            }
        }
    }

    private fun startLocalHistoryAction(project: Project, actionName: String): LocalHistoryAction = LocalHistory.getInstance().startAction(actionName)
}
