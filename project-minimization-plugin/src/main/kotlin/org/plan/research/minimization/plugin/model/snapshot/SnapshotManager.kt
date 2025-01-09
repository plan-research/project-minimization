package org.plan.research.minimization.plugin.model.snapshot

import org.plan.research.minimization.plugin.errors.SnapshotError
import org.plan.research.minimization.plugin.model.context.IJDDContext

import arrow.core.Either
import arrow.core.raise.Raise
import org.plan.research.minimization.plugin.model.context.IJDDContextMonad

typealias TransactionResult<T> = Either<SnapshotError<T>, Unit>
typealias TransactionAction<T, C> = suspend context(IJDDContextMonad<C>, Raise<T>) () -> Unit

/**
 * The `SnapshotManager` interface provides a mechanism to handle transactions within a given context.
 */
interface SnapshotManager {
    /**
     * Executes a transaction within the provided context.
     *
     * @param action A suspendable lambda function representing the transaction action to be performed.
     * @return Either a `SnapshotError` if the transaction fails, or the updated `IJDDContext` if the transaction succeeds.
     */
    context(IJDDContextMonad<C>)
    suspend fun <T, C : IJDDContext> transaction(
        action: TransactionAction<T, C>
    ): TransactionResult<T>
}
