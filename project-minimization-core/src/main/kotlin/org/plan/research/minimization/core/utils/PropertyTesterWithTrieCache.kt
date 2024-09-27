package org.plan.research.minimization.core.utils

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.DDContext
import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.core.model.PropertyTester

class PropertyTesterWithTrieCache<C : DDContext, T : DDItem>(
    private val innerTester: PropertyTester<C, T>
) : PropertyTester<C, T> {
    private val cache = TrieCache<T, PropertyTestResult<C>>()

    override suspend fun test(context: C, items: List<T>): PropertyTestResult<C> {
        return cache.getOrPut(items) { innerTester.test(context, items) }
    }
}

fun <C : DDContext, T : DDItem> PropertyTester<C, T>.withTrieCache(): PropertyTester<C, T> =
    PropertyTesterWithTrieCache(this)
