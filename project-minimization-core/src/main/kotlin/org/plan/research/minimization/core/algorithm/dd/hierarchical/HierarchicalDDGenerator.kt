package org.plan.research.minimization.core.algorithm.dd.hierarchical

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.DDContext
import org.plan.research.minimization.core.model.PropertyTester

data class HDDLevel<C : DDContext, T : DDItem>(
    val context: C, val items: List<T>,
    val propertyTester: PropertyTester<C, T>
)

interface HierarchicalDDGenerator<C : DDContext, T : DDItem> {
    suspend fun generateFirstLevel(): HDDLevel<C, T>
    suspend fun generateNextLevel(minimizationResult: DDAlgorithmResult<C, T>): HDDLevel<C, T>?
}
