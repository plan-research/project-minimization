package org.plan.research.minimization.plugin.services

import arrow.core.Either
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.plan.research.minimization.plugin.errors.SnapshotError
import org.plan.research.minimization.plugin.getSnapshotManager
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.snapshot.SnapshotManager
import org.plan.research.minimization.plugin.model.snapshot.TransactionBody
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings

@Service(Service.Level.PROJECT)
class SnapshotManagerService(private val rootProject: Project) : SnapshotManager {
    private val underlyingObject: SnapshotManager
        get() = rootProject
            .service<MinimizationPluginSettings>()
            .state.snapshotStrategy
            .getSnapshotManager(rootProject)

    override suspend fun <T> transaction(
        context: IJDDContext,
        action: suspend TransactionBody<T>.(newContext: IJDDContext) -> IJDDContext
    ): Either<SnapshotError<T>, IJDDContext> = underlyingObject.transaction(context, action)
}
