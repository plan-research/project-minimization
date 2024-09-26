package org.plan.research.minimization.core.utils

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.core.model.PropertyTester

class PropertyTesterWithTrieCache<T : DDItem>(
    private val innerTester: PropertyTester<T>
) : PropertyTester<T> {
    private val cache = TrieCache<T, PropertyTestResult>()

    override suspend fun test(items: List<T>): PropertyTestResult {
        return cache.getOrPut(items) { innerTester.test(items) }
    }
}

fun <T : DDItem> PropertyTester<T>.withTrieCache(): PropertyTester<T> =
    PropertyTesterWithTrieCache(this)
