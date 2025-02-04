package org.plan.research.minimization.plugin.context.snapshot

import org.plan.research.minimization.plugin.context.IJDDContextBase

/**
 * The `SnapshotManager` interface provides a mechanism to handle transactions within a given context.
 */
interface SnapshotManager {
    suspend fun <C : IJDDContextBase<C>> createMonad(context: C): SnapshotMonad<C>
}
