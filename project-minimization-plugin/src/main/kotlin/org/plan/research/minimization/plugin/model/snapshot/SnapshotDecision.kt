package org.plan.research.minimization.plugin.model.snapshot

sealed interface SnapshotDecision {
    data object Commit : SnapshotDecision
    data object Rollback : SnapshotDecision
}