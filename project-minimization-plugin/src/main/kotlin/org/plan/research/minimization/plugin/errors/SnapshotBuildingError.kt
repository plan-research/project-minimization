package org.plan.research.minimization.plugin.errors

sealed interface SnapshotBuildingError {
    data object NoPreviousSnapshot : SnapshotBuildingError
    data object RollbackFailed : SnapshotBuildingError
    data object CopyingFailed: SnapshotBuildingError
    data object Aborted : SnapshotBuildingError
}