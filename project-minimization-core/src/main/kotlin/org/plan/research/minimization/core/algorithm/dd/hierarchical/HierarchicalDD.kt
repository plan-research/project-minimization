package org.plan.research.minimization.core.algorithm.dd.hierarchical

import kotlinx.coroutines.yield
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.DDVersion

class HierarchicalDD(private val baseDDAlgorithm: DDAlgorithm) {
    suspend fun <V : DDVersion, T : DDItem> minimize(generator: HierarchicalDDGenerator<V, T>): V {
        var level = generator.generateFirstLevel()
        while (true) {
            yield()
            val minimizedLevel = baseDDAlgorithm.minimize(level.version, level.items, level.propertyTester)
            level = generator.generateNextLevel(minimizedLevel) ?: break
        }
        return level.version
    }
}
