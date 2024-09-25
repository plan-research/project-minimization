package org.plan.research.minimization.core.algorithm.dd.hierarchical

import kotlinx.coroutines.yield
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.core.model.DDItem

class HierarchicalDD(private val baseDDAlgorithm: DDAlgorithm) {
    suspend fun <T : DDItem> minimize(generator: HierarchicalDDGenerator<T>) {
        var level: HDDLevel<T>? = generator.generateFirstLevel()
        while (level != null) {
            yield()
            val minimizedLevel = baseDDAlgorithm.minimize(level.items, level.propertyTester)
            level = generator.generateNextLevel(minimizedLevel)
        }
    }
}