package org.plan.research.minimization.plugin.services

import arrow.core.Either
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.plan.research.minimization.plugin.errors.SnapshotBuildingError
import org.plan.research.minimization.plugin.getSnapshotStrategy
import org.plan.research.minimization.plugin.model.snapshot.Snapshot
import org.plan.research.minimization.plugin.model.snapshot.SnapshotBuilder
import org.plan.research.minimization.plugin.model.snapshot.SnapshotDecision
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings

@Service(Service.Level.PROJECT)
class SnapshottingService(project: Project): SnapshotBuilder<Snapshot> {
    private val underlyingObject: SnapshotBuilder<Snapshot> = project
        .service<MinimizationPluginSettings>()
        .state
        .currentSnapshotStrategy
        .getSnapshotStrategy(project) as SnapshotBuilder<Snapshot>
    override suspend fun makeTransaction(
        currentSnapshot: Snapshot,
        modifier: suspend (Project) -> SnapshotDecision
    ): Either<SnapshotBuildingError, Snapshot> = underlyingObject.makeTransaction(currentSnapshot, modifier)

    override fun initialSnapshot(): Either<SnapshotBuildingError, Snapshot> = underlyingObject.initialSnapshot()
}