package org.plan.research.minimization.core.algorithm.dd.impl

import kotlinx.coroutines.yield
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.core.model.PropertyTester
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.math.exp

/**
 * Probabilistic Delta Debugging.
 *
 * This version doesn't support caching mechanisms.
 */
class ProbabilisticDD : DDAlgorithm {
    override suspend fun <T : DDItem> minimize(items: List<T>, propertyTester: PropertyTester<T>): List<T> {
        val buffer = ArrayDeque<T>()
        val probs = IdentityHashMap<T, Double>()
        val defaultProb = 1 - exp(-2.0 / items.size)
        items.forEach { item -> probs[item] = defaultProb }
        val currentItems = items.toMutableList()
        val excludedItems = mutableListOf<T>()
        while (currentItems.size > 1 && probs[currentItems.last()]!! < 1.0) {
            yield()
            var p = 1.0
            while (currentItems.size > 1 && p > 0.0) {
                val nextP = p * (1.0 - probs[currentItems.last()]!!)
                if (nextP * (excludedItems.size + 1) >= p * excludedItems.size) {
                    excludedItems.add(currentItems.removeLast())
                    p = nextP
                } else {
                    break
                }
            }
            if (propertyTester.test(currentItems) == PropertyTestResult.PRESENT) {
                excludedItems.clear()
                continue
            }
            excludedItems.forEach { item -> probs.computeIfPresent(item) { _, v -> v / (1 - p) } }
            merge(probs, buffer, currentItems, excludedItems)
        }
        return currentItems
    }

    private fun <T> merge(
        probs: Map<T, Double>, buffer: ArrayDeque<T>,
        currentItems: MutableList<T>, excludedItems: MutableList<T>
    ) {
        var index = 0
        buffer.clear()
        while (excludedItems.isNotEmpty()) {
            val itemToAdd = if (buffer.isNotEmpty()) {
                if (probs[buffer.first()]!! < probs[excludedItems.last()]!!) {
                    excludedItems.removeLast()
                } else {
                    buffer.removeFirst()
                }
            } else {
                if (index >= currentItems.size || probs[currentItems[index]]!! < probs[excludedItems.last()]!!) {
                    excludedItems.removeLast()
                } else {
                    null
                }
            }
            itemToAdd?.let {
                if (index < currentItems.size) {
                    buffer.add(currentItems[index])
                    currentItems[index] = it
                } else {
                    currentItems.add(it)
                }
            }
            index += 1
        }
        while (index < currentItems.size) {
            buffer.add(currentItems[index])
            currentItems[index] = buffer.removeFirst()
            index += 1
        }
        currentItems.addAll(buffer)
        buffer.clear()
    }
}