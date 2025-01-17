package org.plan.research.minimization.core.algorithm.dd

import org.plan.research.minimization.core.model.*

data class DDAlgorithmResult<T>(val survived: List<T>, val deleted: List<T>)

/**
 * Interface representing a Delta Debugging algorithm used to minimize a set of items.
 *
 * The `minimize` function works with a given context and a list of items, using a property tester to
 * identify and minimize the subset of items that cause a specific error or property.
 */
interface DDAlgorithm {
    context(M)
    suspend fun <M : Monad, T : DDItem> minimize(
        items: List<T>,
        propertyTester: PropertyTester<M, T>,
    ): DDAlgorithmResult<T>
}
