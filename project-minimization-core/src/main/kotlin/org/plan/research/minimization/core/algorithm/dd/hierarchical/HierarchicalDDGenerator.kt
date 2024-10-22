package org.plan.research.minimization.core.algorithm.dd.hierarchical

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.model.DDContext
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.PropertyTester

import arrow.core.Option

/**
 * Represents a level in a hierarchical delta debugging process.
 *
 * @param C The type of context that provides information relevant to the delta debugging process.
 * @param T The type of items being analyzed and manipulated in the delta debugging process.
 * @property context The context in which the current level of analysis is taking place.
 * @property items A list of items present at this level of the hierarchical structure.
 * @property propertyTester An instance of [PropertyTester] to evaluate properties on the context and items.
 */
data class HDDLevel<C : DDContext, T : DDItem>(
    val context: C, val items: List<T>,
    val propertyTester: PropertyTester<C, T>,
)

/**
 * Interface representing a generator for hierarchical delta debugging.
 * Responsible for generating the first and subsequent levels of the hierarchy.
 *
 * @param C The type of context associated with the hierarchical delta debugging process.
 * @param T The type of items being analyzed and manipulated.
 */
interface HierarchicalDDGenerator<C : DDContext, T : DDItem> {
    suspend fun generateFirstLevel(context: C): Option<HDDLevel<C, T>>
    suspend fun generateNextLevel(minimizationResult: DDAlgorithmResult<C, T>): Option<HDDLevel<C, T>>
}
