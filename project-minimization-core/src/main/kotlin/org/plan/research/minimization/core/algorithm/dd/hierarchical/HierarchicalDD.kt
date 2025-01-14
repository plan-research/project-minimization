package org.plan.research.minimization.core.algorithm.dd.hierarchical

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.core.model.DDItem

import arrow.core.getOrElse

import kotlinx.coroutines.yield
import org.plan.research.minimization.core.algorithm.dd.ReversedDDAlgorithm
import org.plan.research.minimization.core.model.Monad

/**
 * The `HierarchicalDD` class provides a wrapper around a base delta debugging algorithm to facilitate
 * hierarchical delta debugging.
 *
 * This class makes use of a `baseDDAlgorithm` and operates with a `HierarchicalDDGenerator` to minimize
 * a given context through multiple hierarchical levels generated iteratively.
 *
 * @param baseDDAlgorithm The delta debugging algorithm utilized for minimization at each hierarchical level.
 */
class HierarchicalDD(private val baseDDAlgorithm: DDAlgorithm) {
    context(M)
    suspend fun <M : Monad, T : DDItem> minimize(generator: HierarchicalDDGenerator<M, T>) {
        var level = generator.generateFirstLevel().getOrElse { return }
        while (true) {
            yield()
            val minimizedLevel = baseDDAlgorithm.minimize(level.items, level.propertyTester)
            level = generator.generateNextLevel(minimizedLevel).getOrElse { return }
        }
    }
}

class ReversedHierarchicalDD(private val baseDDAlgorithm: ReversedDDAlgorithm) {
    context(M)
    suspend fun <M : Monad, T : DDItem> minimize(generator: ReversedHierarchicalDDGenerator<M, T>) {
        var level = generator.generateFirstLevel().getOrElse { return }
        while (true) {
            yield()
            val minimizedLevel = baseDDAlgorithm.minimize(level.items, level.propertyTester)
            level = generator.generateNextLevel(minimizedLevel).getOrElse { return }
        }
    }
}
