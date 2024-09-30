package org.plan.research.minimization.plugin.snapshot

import arrow.core.raise.either
import arrow.core.raise.ensure
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.testFramework.utils.vfs.deleteRecursively
import org.plan.research.minimization.plugin.errors.SnapshotBuildingError
import org.plan.research.minimization.plugin.model.snapshot.Snapshot

class CloneSnapshot(
    override val project: Project,
    override val previousSnapshot: CloneSnapshot?,
) : Snapshot {
    override suspend fun rollback() = either {
        if (previousSnapshot == null) { // Root case, I don't want to throw an error
            return@either this@CloneSnapshot
        }
        val thisProjectRoot = project.guessProjectDir()
        ApplicationManager.getApplication().invokeLater {
            ensure(ProjectManager.getInstance().closeAndDispose(project)) { SnapshotBuildingError.RollbackFailed }
        }
        thisProjectRoot?.let {
            writeAction {
                it.deleteRecursively()
            }
        }
        previousSnapshot
    }
}