package org.plan.research.minimization.plugin.model.snapshot

import arrow.core.raise.Raise

/**
 * @param T
 * @param raise
 */
class TransactionBody<T>(
    raise: Raise<T>,
) : Raise<T> by raise
