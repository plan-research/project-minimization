package org.plan.research.minimization.plugin.errors

sealed interface SnapshotError<T> {
    data class Aborted<T>(val reason: T) : SnapshotError<T>
    data class TransactionFailed<T>(val error: Throwable) : SnapshotError<T>
    data class TransactionCreationFailed<T>(val reason: String) : SnapshotError<T>
}
