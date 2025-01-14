package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.getSnapshotManager
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.monad.IJDDContextMonad
import org.plan.research.minimization.plugin.model.snapshot.SnapshotManager
import org.plan.research.minimization.plugin.model.snapshot.TransactionAction
import org.plan.research.minimization.plugin.model.snapshot.TransactionResult

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class SnapshotManagerService(private val rootProject: Project) : SnapshotManager {
    private val underlyingObject: SnapshotManager by rootProject
        .service<MinimizationPluginSettings>()
        .stateObservable
        .snapshotStrategy
        .observe { it.getSnapshotManager(rootProject) }

    context(IJDDContextMonad<C>)
    override suspend fun <T, C : IJDDContext> transaction(
        action: TransactionAction<T, C>,
    ): TransactionResult<T> = underlyingObject.transaction(action)
}
