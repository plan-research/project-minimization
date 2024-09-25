package org.plan.research.minimization.core.algorithm.dd.hierarchical

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.PropertyTester

data class HDDLevel<T: DDItem>(val items: List<T>, val propertyTester: PropertyTester<T>)

interface HierarchicalDDGenerator<T: DDItem> {
    suspend fun generateFirstLevel(): HDDLevel<T>
    suspend fun generateNextLevel(minimizedLevel: List<T>): HDDLevel<T>?
}