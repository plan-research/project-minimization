package org.plan.research.minimization.plugin.model

import arrow.core.Either
import arrow.core.raise.OptionRaise
import org.plan.research.minimization.plugin.errors.SnapshotError


/**
 * The `SnapshotManager` interface provides a mechanism to handle transactions within a given context.
 */
interface SnapshotManager {

    /**
     * Executes a transaction within the provided context. It should create a copy of the context with a new project.
     *
     * @param context The context in which the transaction is executed.
     * @param action A suspendable lambda function representing the transaction action to be performed.
     * @return Either a `SnapshotError` if the transaction fails, or the updated `IJDDContext` if the transaction succeeds.
     */
    suspend fun transaction(
        context: IJDDContext,
        action: suspend OptionRaise.(newContext: IJDDContext) -> IJDDContext,
    ): Either<SnapshotError, IJDDContext>
}
