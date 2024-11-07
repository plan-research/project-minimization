package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.getSnapshotManager
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.snapshot.SnapshotManager
import org.plan.research.minimization.plugin.model.snapshot.TransactionBody
import org.plan.research.minimization.plugin.model.snapshot.TransactionResult

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class SnapshotManagerService(private val rootProject: Project) : SnapshotManager {
    private val underlyingObject: SnapshotManager by rootProject
        .service<MinimizationPluginSettings>()
        .state
        .stateObservable
        .snapshotStrategy
        .observe { it.getSnapshotManager(rootProject) }

    override suspend fun <T, C : IJDDContext> transaction(
        context: C,
        action: suspend TransactionBody<T>.(newContext: C) -> C,
    ): TransactionResult<T, C> = underlyingObject.transaction(context, action)
}
