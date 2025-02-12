package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.model.context.IJDDContext

import com.intellij.history.Label
import com.intellij.history.LocalHistory
import com.intellij.history.LocalHistoryAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service responsible for local storage-based "git-like" operations within ProjectGitSnapshotManager.
 */
@Service(Service.Level.APP)
class LocalStorageWrapperService {
    var lastLabel: Label? = null

    suspend fun commitChanges(context: IJDDContext): IJDDContext = withContext(Dispatchers.IO) {
        val project = context.indexProject

        val action = startLocalHistoryAction(project, "Save Project Snapshot")
        try {
            lastLabel = LocalHistory.getInstance().putSystemLabel(project, "Project Snapshot")
        } finally {
            action.finish()
        }
        context.apply { projectDir.refresh(false, true) }
    }

    suspend fun resetChanges(context: IJDDContext) {
        val project = context.indexProject

        withContext(Dispatchers.IO) {
            val action = startLocalHistoryAction(project, "Revert Project Snapshot")
            try {
                // Get Local History revisions for the project directory
                lastLabel?.revert(project, context.indexProjectDir)
            } finally {
                action.finish()
            }
        }
        context.projectDir.refresh(false, true)
    }

    suspend fun gitInit(project: Project) {
        withContext(Dispatchers.IO) {
            // No explicit initialization needed for Local History
            lastLabel = LocalHistory.getInstance().putSystemLabel(project, "Initialized Local History Tracking")
        }
    }

    private fun startLocalHistoryAction(project: Project, actionName: String): LocalHistoryAction = LocalHistory.getInstance().startAction(actionName)
}
