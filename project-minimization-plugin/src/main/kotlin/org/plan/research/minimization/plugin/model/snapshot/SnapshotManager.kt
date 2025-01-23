package org.plan.research.minimization.plugin.model.snapshot

import org.plan.research.minimization.plugin.model.context.IJDDContextBase

import org.plan.research.minimization.plugin.model.monad.SnapshotMonad

/**
 * The `SnapshotManager` interface provides a mechanism to handle transactions within a given context.
 */
interface SnapshotManager {
    suspend fun <C : IJDDContextBase<C>> createMonad(context: C): SnapshotMonad<C>
}
