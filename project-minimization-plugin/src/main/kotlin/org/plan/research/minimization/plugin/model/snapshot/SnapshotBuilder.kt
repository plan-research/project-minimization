package org.plan.research.minimization.plugin.model.snapshot

import arrow.core.Either
import com.intellij.openapi.project.Project
import org.plan.research.minimization.plugin.errors.SnapshotBuildingError

/**
 * Interface for building snapshots of a project.
 *
 * @param S the type of snapshot being managed, which must implement the [Snapshot] interface.
 */
interface SnapshotBuilder<S : Snapshot> {
    suspend fun makeTransaction(
        currentSnapshot: S,
        modifier: suspend (Project) -> SnapshotDecision
    ): Either<SnapshotBuildingError, S>

    fun initialSnapshot(): Either<SnapshotBuildingError, S>
}