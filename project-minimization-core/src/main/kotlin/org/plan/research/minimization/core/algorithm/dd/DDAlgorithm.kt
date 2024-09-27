package org.plan.research.minimization.core.algorithm.dd

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.DDVersion
import org.plan.research.minimization.core.model.PropertyTester

interface DDAlgorithm {
    suspend fun <V : DDVersion, T : DDItem> minimize(
        version: V, items: List<T>,
        propertyTester: PropertyTester<V, T>
    ): DDAlgorithmResult<V, T>
}

data class DDAlgorithmResult<V : DDVersion, T : DDItem>(
    val version: V,
    val items: List<T>,
)
