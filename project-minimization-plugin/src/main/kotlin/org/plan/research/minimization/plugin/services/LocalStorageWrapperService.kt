package org.plan.research.minimization.plugin.services

import com.intellij.history.LocalHistory
import com.intellij.history.LocalHistoryAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.plan.research.minimization.plugin.model.context.IJDDContext

/**
 * Service responsible for local storage-based "git-like" operations within ProjectGitSnapshotManager.
 */
@Service(Service.Level.APP)
class LocalStorageWrapperService {
//
//    suspend fun commitChanges(context: IJDDContext, project: Project): IJDDContext = withContext(Dispatchers.IO) {
//        val action = startLocalHistoryAction(project, "Save Project Snapshot")
//        try {
//            LocalHistory.getInstance().putSystemLabel(project, "Project Snapshot")
//        } finally {
//            action.finish()
//        }
//        context.apply { projectDir.refresh(false, true) }
//    }
//
//    suspend fun resetChanges(context: IJDDContext, project: Project) {
//        withContext(Dispatchers.IO) {
//            val action = startLocalHistoryAction(project, "Revert Project Snapshot")
//            try {
//                // Get Local History revisions for the project directory
//                LocalHistory.getInstance().
//                val revisions = LocalHistory.getInstance().getRevisionsFor(context.projectDir)
//
//                if (!revisions.isNullOrEmpty()) {
//                    val lastRevision = revisions.first() // Most recent version
//                    runWriteAction {
//                        lastRevision.restore()
//                    }
//                }
//            } finally {
//                action.finish()
//            }
//        }
//        context.projectDir.refresh(false, true)
//    }
//
//    suspend fun gitInit(virtualProjectDir: VirtualFile, project: Project) {
//        withContext(Dispatchers.IO) {
//            // No explicit initialization needed for Local History
//            LocalHistory.getInstance().putSystemLabel(project, "Initialized Local History Tracking")
//        }
//    }
//
//    private fun startLocalHistoryAction(project: Project, actionName: String): LocalHistoryAction {
//        return LocalHistory.getInstance().startAction(actionName)
//    }
}

