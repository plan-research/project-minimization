package org.plan.research.minimization.plugin.errors

sealed interface SnapshotError<out T> {
    data class Aborted<T>(val reason: T) : SnapshotError<T>
    data class TransactionFailed(val error: Throwable) : SnapshotError<Nothing>
    data class TransactionCreationFailed(val reason: String) : SnapshotError<Nothing>
}
