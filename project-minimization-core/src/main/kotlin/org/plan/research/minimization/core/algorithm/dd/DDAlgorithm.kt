package org.plan.research.minimization.core.algorithm.dd

import org.plan.research.minimization.core.model.DDContext
import org.plan.research.minimization.core.model.DDContextMonad
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.PropertyTester

typealias DDAlgorithmResult<T> = List<T>

/**
 * Interface representing a Delta Debugging algorithm used to minimize a set of items.
 *
 * The `minimize` function works with a given context and a list of items, using a property tester to
 * identify and minimize the subset of items that cause a specific error or property.
 */
interface DDAlgorithm {
    context(M)
    suspend fun <M : DDContextMonad<C>, C : DDContext, T : DDItem> minimize(
        items: List<T>,
        propertyTester: PropertyTester<M, C, T>,
    ): DDAlgorithmResult<T>
}
