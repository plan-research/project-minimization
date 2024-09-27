package org.plan.research.minimization.core.algorithm.dd.hierarchical

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.DDVersion
import org.plan.research.minimization.core.model.PropertyTester

data class HDDLevel<V : DDVersion, T : DDItem>(
    val version: V, val items: List<T>,
    val propertyTester: PropertyTester<V, T>
)

interface HierarchicalDDGenerator<V : DDVersion, T : DDItem> {
    suspend fun generateFirstLevel(): HDDLevel<V, T>
    suspend fun generateNextLevel(minimizationResult: DDAlgorithmResult<V, T>): HDDLevel<V, T>?
}
