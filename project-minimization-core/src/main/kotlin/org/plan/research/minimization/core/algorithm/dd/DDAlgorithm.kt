package org.plan.research.minimization.core.algorithm.dd

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.DDContext
import org.plan.research.minimization.core.model.PropertyTester

/**
 * Interface representing a Delta Debugging algorithm used to minimize a set of items.
 *
 * The `minimize` function works with a given context and a list of items, using a property tester to
 * identify and minimize the subset of items that cause a specific error or property.
 */
interface DDAlgorithm {
    suspend fun <C : DDContext, T : DDItem> minimize(
        context: C, items: List<T>,
        propertyTester: PropertyTester<C, T>
    ): DDAlgorithmResult<C, T>
}

/**
 * DDAlgorithmResult is a data class representing the result of an algorithm execution.
 *
 * @param C the type of context associated with the algorithm.
 * @param T the type of items produced by the algorithm.
 * @property context the context in which the algorithm was executed.
 * @property items the list of items produced by the algorithm.
 */
data class DDAlgorithmResult<C : DDContext, T : DDItem>(
    val context: C,
    val items: List<T>,
)
