package org.plan.research.minimization.core.algorithm.dd

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.DDContext
import org.plan.research.minimization.core.model.PropertyTester

interface DDAlgorithm {
    suspend fun <C : DDContext, T : DDItem> minimize(
        context: C, items: List<T>,
        propertyTester: PropertyTester<C, T>
    ): DDAlgorithmResult<C, T>
}

data class DDAlgorithmResult<C : DDContext, T : DDItem>(
    val context: C,
    val items: List<T>,
)
