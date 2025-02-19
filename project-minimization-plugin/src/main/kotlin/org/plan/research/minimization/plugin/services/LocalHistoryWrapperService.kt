package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.model.context.IJDDContext

import com.intellij.history.Label
import com.intellij.history.LocalHistory
import com.intellij.history.LocalHistoryAction
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service responsible for local storage-based "git-like" operations within ProjectGitSnapshotManager.
 */
@Service(Service.Level.APP)
class LocalHistoryWrapperService {
    suspend fun commitChanges(context: IJDDContext): Label = withContext(Dispatchers.EDT) {
        val project = context.indexProject

        val action = startLocalHistoryAction(project, "Save Project Snapshot")
        try {
            runWriteAction {
                LocalHistory.getInstance().putSystemLabel(project, "Project Snapshot")
            }
        } finally {
            action.finish()
        }
    }

    suspend fun resetChanges(context: IJDDContext, lastLabel: Label?) {
        val project = context.indexProject

        withContext(Dispatchers.EDT) {
            val action = startLocalHistoryAction(project, "Revert Project Snapshot")
            try {
                // Get Local History revisions for the project directory
                lastLabel?.let {
                    runWriteAction {
                        it.revert(project, context.indexProjectDir)
                    }
                }
            } finally {
                action.finish()
            }
        }
    }

    suspend fun gitInit(project: Project): Label = withContext(Dispatchers.EDT) {
        // No explicit initialization needed for Local History
        runWriteAction {
            LocalHistory.getInstance().putSystemLabel(project, "Initialized Local History Tracking")
        }
    }

    private fun startLocalHistoryAction(project: Project, actionName: String): LocalHistoryAction = LocalHistory.getInstance().startAction(actionName)
}
