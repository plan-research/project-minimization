package org.plan.research.minimization.core.algorithm.dd.hierarchical

import arrow.core.getOrElse
import kotlinx.coroutines.yield
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.DDContext

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
