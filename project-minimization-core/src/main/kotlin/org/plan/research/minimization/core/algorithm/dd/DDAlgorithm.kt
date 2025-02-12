package org.plan.research.minimization.core.algorithm.dd

import org.plan.research.minimization.core.model.*

data class DDAlgorithmResult<T>(val retained: List<T>, val deleted: List<T>)

/**
 * Interface representing a Delta Debugging algorithm used to minimize a set of items.
 *
 * The `minimize` function works with a given context and a list of items, using a property tester to
 * identify and minimize the subset of items that cause a specific error or property.
 */
interface DDAlgorithm<T : DDItem> {
    context(M)
    suspend fun <M : Monad> minimize(
        items: List<T>,
        propertyTester: PropertyTester<M, T>,
    ): DDAlgorithmResult<T>
}
