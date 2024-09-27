package org.plan.research.minimization.core.utils

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.DDVersion
import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.core.model.PropertyTester

class PropertyTesterWithTrieCache<V : DDVersion, T : DDItem>(
    private val innerTester: PropertyTester<V, T>
) : PropertyTester<V, T> {
    private val cache = TrieCache<T, PropertyTestResult<V>>()

    override suspend fun test(version: V, items: List<T>): PropertyTestResult<V> {
        return cache.getOrPut(items) { innerTester.test(version, items) }
    }
}

fun <V : DDVersion, T : DDItem> PropertyTester<V, T>.withTrieCache(): PropertyTester<V, T> =
    PropertyTesterWithTrieCache(this)
