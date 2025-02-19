package org.plan.research.minimization.core.algorithm.dd.impl

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.model.*

import java.util.*

import kotlin.collections.ArrayDeque
import kotlin.math.min
import kotlinx.coroutines.yield

/**
 * Probabilistic Delta Debugging.
 *
 * This version doesn't support caching mechanisms.
 */
@Suppress("MAGIC_NUMBER", "FLOAT_IN_ACCURATE_CALCULATIONS")
class ProbabilisticDD : DDAlgorithm {
    context(M)
    override suspend fun <M : Monad, T : DDItem> minimize(
        items: List<T>,
        propertyTester: PropertyTester<M, T>,
        info: DDInfo<T>,
    ): DDAlgorithmResult<T> {
        val buffer = ArrayDeque<T>()
        val probs = initProbs(items, info)
        val currentItems = items.sortedByDescending { probs[it]!! }.toMutableList()
        val excludedItems = mutableListOf<T>()
        val deletedItems = mutableListOf<T>()
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
            propertyTester.test(currentItems, excludedItems).fold(
                ifLeft = {
                    excludedItems.forEach { item -> probs.computeIfPresent(item) { _, v -> v / (1 - p) } }
                    merge(probs, buffer, currentItems, excludedItems)
                },
                ifRight = {
                    deletedItems.addAll(excludedItems)
                    excludedItems.clear()
                },
            )
        }
        return DDAlgorithmResult(currentItems, deletedItems)
    }

    private fun <T> merge(
        probs: Map<T, Double>,
        buffer: ArrayDeque<T>,
        currentItems: MutableList<T>,
        excludedItems: MutableList<T>,
    ) {
        buffer.clear()
        var index = 0
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

    private fun <T : DDItem> initProbs(
        items: List<T>,
        info: DDInfo<T>,
    ): IdentityHashMap<T, Double> {
        val probs = IdentityHashMap<T, Double>()
        val important = info.importanceOf(items)

        val unimportantProb = initProb(items.size)
        val importantItemsCount = items.count { important[it]!! }
        val importantProb = initProb(importantItemsCount)

        items.forEach {
            probs[it] = if (important[it]!!) importantProb else unimportantProb
        }

        return probs
    }

    companion object {
        // Typical expected size of the minimized result
        private const val EXPECTED_RESULT_SIZE = 2

        private fun initProb(size: Int): Double = if (size <= 0) {
            1.0
        } else {
            min(
                EXPECTED_RESULT_SIZE / size.toDouble(),
                // To not assign p >= 1 in corner cases
                EXPECTED_RESULT_SIZE / (EXPECTED_RESULT_SIZE + 1).toDouble(),
            )
        }
    }
}
