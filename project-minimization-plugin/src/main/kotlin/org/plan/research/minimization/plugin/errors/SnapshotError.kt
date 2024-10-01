package org.plan.research.minimization.plugin.errors

sealed interface SnapshotError {
    data object Aborted : SnapshotError
    data class TransactionFailed(val error: Throwable) : SnapshotError
}
