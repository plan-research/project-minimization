package org.plan.research.minimization.core.algorithm.dd.impl

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.algorithm.dd.ReversedDDAlgorithm
import org.plan.research.minimization.core.model.*
import java.util.*

class ReversedDDAlgorithmAdapter(private val ddAlgorithm: DDAlgorithm) : ReversedDDAlgorithm {
    private class PropertyTesterAdapter<M : Monad, T : DDItem>(
        private val tester: ReversedPropertyTester<M, T>,
        currentItems: List<T>,
    ) : PropertyTester<M, T> {
        private val currentItems = IdentityHashMap(currentItems.associateWith { })

        context(M)
        override suspend fun test(items: List<T>): PropertyTestResult {
            val itemsToDelete = currentItems.keys.minus(items.toSet())
            return tester.test(itemsToDelete.toList()).onRight {
                itemsToDelete.forEach { currentItems.remove(it) }
            }
        }
    }

    context(M)
    override suspend fun <M : Monad, T : DDItem> minimize(
        items: List<T>,
        propertyTester: ReversedPropertyTester<M, T>,
    ): DDAlgorithmResult<T> {
        val adapter = PropertyTesterAdapter(propertyTester, items)
        return ddAlgorithm.minimize(items, adapter)
    }
}

fun DDAlgorithm.asReversed(): ReversedDDAlgorithm = ReversedDDAlgorithmAdapter(this)
