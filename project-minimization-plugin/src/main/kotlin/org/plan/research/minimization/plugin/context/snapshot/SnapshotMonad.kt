package org.plan.research.minimization.plugin.context.snapshot

import org.plan.research.minimization.core.model.Monad
import org.plan.research.minimization.core.model.MonadF
import org.plan.research.minimization.plugin.context.IJDDContext
import org.plan.research.minimization.plugin.context.IJDDContextMonad

import arrow.core.Either
import arrow.core.raise.Raise

typealias TransactionResult<T> = Either<SnapshotError<T>, Unit>
typealias TransactionAction<T, C> = suspend context(IJDDContextMonad<C>, Raise<T>) () -> Unit
typealias SnapshotMonadF<C, T> = MonadF<SnapshotMonad<C>, T>

interface SnapshotMonad<C : IJDDContext> : Monad {
    val context: C

    suspend fun <T> transaction(action: TransactionAction<T, C>): TransactionResult<T>
}
