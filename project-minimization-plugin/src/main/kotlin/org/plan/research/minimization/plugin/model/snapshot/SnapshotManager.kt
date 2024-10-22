package org.plan.research.minimization.plugin.model.snapshot

import org.plan.research.minimization.plugin.errors.SnapshotError
import org.plan.research.minimization.plugin.model.IJDDContext

import arrow.core.Either

typealias TransactionResult<T> = Either<SnapshotError<T>, IJDDContext>

/**
 * The `SnapshotManager` interface provides a mechanism to handle transactions within a given context.
 */
interface SnapshotManager {
    /**
     * Executes a transaction within the provided context.
     *
     * @param context The context in which the transaction is executed.
     * @param action A suspendable lambda function representing the transaction action to be performed.
     * @return Either a `SnapshotError` if the transaction fails, or the updated `IJDDContext` if the transaction succeeds.
     */
    suspend fun <T> transaction(
        context: IJDDContext,
        action: suspend TransactionBody<T>.(newContext: IJDDContext) -> IJDDContext,
    ): TransactionResult<T>
}
