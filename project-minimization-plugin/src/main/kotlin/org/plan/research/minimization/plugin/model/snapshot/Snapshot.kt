package org.plan.research.minimization.plugin.model.snapshot

import arrow.core.Either
import com.intellij.openapi.project.Project
import org.plan.research.minimization.plugin.errors.SnapshotBuildingError

interface Snapshot {
    val project: Project
    val previousSnapshot: Snapshot?
    suspend fun rollback(): Either<SnapshotBuildingError, Snapshot>
}