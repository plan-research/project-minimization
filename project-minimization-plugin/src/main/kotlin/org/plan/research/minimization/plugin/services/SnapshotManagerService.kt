package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.context.IJDDContextBase
import org.plan.research.minimization.plugin.context.snapshot.SnapshotManager
import org.plan.research.minimization.plugin.context.snapshot.SnapshotMonad
import org.plan.research.minimization.plugin.util.getSnapshotManager

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

    override suspend fun <C : IJDDContextBase<C>> createMonad(context: C): SnapshotMonad<C> =
        underlyingObject.createMonad(context)
}
