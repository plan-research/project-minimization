package org.plan.research.minimization.core.algorithm.dd

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.PropertyTester

interface DDAlgorithm {
    suspend fun <T: DDItem> minimize(items: List<T>, propertyTester: PropertyTester<T>): List<T>
}
