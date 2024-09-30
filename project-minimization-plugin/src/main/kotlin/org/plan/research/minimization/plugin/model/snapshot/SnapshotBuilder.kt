package org.plan.research.minimization.plugin.model.snapshot

import arrow.core.Either
import com.intellij.openapi.project.Project
import org.plan.research.minimization.plugin.errors.SnapshotBuildingError

interface SnapshotBuilder<S : Snapshot> {
    suspend fun makeTransaction(
        currentSnapshot: S,
        modifier: suspend (Project) -> Boolean
    ): Either<SnapshotBuildingError, S>

    fun initialSnapshot(): Either<SnapshotBuildingError, S>
}