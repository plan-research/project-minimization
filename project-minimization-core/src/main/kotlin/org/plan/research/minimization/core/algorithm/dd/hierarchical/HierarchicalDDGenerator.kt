package org.plan.research.minimization.core.algorithm.dd.hierarchical

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.model.*

import arrow.core.Option

/**
 * Represents a level in a hierarchical delta debugging process.
 *
 * @param T The type of items being analyzed and manipulated in the delta debugging process.
 * @property items A list of items present at this level of the hierarchical structure.
 * @property propertyTester An instance of [PropertyTester] to evaluate properties on the context and items.
 */
data class HDDLevel<M : Monad, T : DDItem>(
    val items: List<T>,
    val propertyTester: PropertyTester<M, T>,
)

/**
 * Interface representing a generator for hierarchical delta debugging.
 * Responsible for generating the first and subsequent levels of the hierarchy.
 *
 * @param T The type of items being analyzed and manipulated.
 */
interface HierarchicalDDGenerator<M : MonadT<M2>, M2 : Monad, T : DDItem> {
    context(M)
    suspend fun generateFirstLevel(): Option<HDDLevel<M2, T>>

    context(M)
    suspend fun generateNextLevel(minimizationResult: DDAlgorithmResult<T>): Option<HDDLevel<M2, T>>
}

data class ReversedHDDLevel<M : Monad, T : DDItem>(
    val items: List<T>,
    val propertyTester: ReversedPropertyTester<M, T>,
)

@Suppress("TYPE_ALIAS")
interface ReversedHierarchicalDDGenerator<M : MonadT<M2>, M2 : Monad, T : DDItem> {
    context(M)
    suspend fun generateFirstLevel(): Option<ReversedHDDLevel<M2, T>>

    context(M)
    suspend fun generateNextLevel(minimizationResult: DDAlgorithmResult<T>): Option<ReversedHDDLevel<M2, T>>
}
