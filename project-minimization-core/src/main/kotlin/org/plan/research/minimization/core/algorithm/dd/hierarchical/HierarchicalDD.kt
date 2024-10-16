package org.plan.research.minimization.core.algorithm.dd.hierarchical

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.core.model.DDContext
import org.plan.research.minimization.core.model.DDItem

import arrow.core.getOrElse

import kotlinx.coroutines.yield

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
    suspend fun <C : DDContext, T : DDItem> minimize(context: C, generator: HierarchicalDDGenerator<C, T>): C {
        var level = generator.generateFirstLevel(context).getOrElse { return context }
        while (true) {
            yield()
            val minimizedLevel = baseDDAlgorithm.minimize(level.context, level.items, level.propertyTester)
            level = generator.generateNextLevel(minimizedLevel).getOrElse { return minimizedLevel.context }
        }
    }
}
