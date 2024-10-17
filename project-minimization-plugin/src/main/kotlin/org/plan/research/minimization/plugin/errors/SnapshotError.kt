package org.plan.research.minimization.plugin.errors

/**
 * @param T
 */
sealed interface SnapshotError<out T> {
    /**
     * @param T
     * @property reason
     */
    data class Aborted<T>(val reason: T) : SnapshotError<T>

    /**
     * @property error
     */
    data class TransactionFailed(val error: Throwable) : SnapshotError<Nothing>

    /**
     * @property reason
     */
    data class TransactionCreationFailed(val reason: String) : SnapshotError<Nothing>
}
