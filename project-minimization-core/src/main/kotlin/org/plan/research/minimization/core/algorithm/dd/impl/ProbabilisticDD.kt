package org.plan.research.minimization.core.algorithm.dd.impl

import org.plan.research.minimization.core.algorithm.dd.IDDAlgorithm
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.model.*

import java.util.*

import kotlin.collections.ArrayDeque
import kotlin.math.exp
import kotlinx.coroutines.yield

/**
 * Probabilistic Delta Debugging.
 *
 * This version doesn't support caching mechanisms.
 */
@Suppress("MAGIC_NUMBER", "FLOAT_IN_ACCURATE_CALCULATIONS")
class ProbabilisticDD<U : DDItem>(
    private val initDist: Distribution<U> = ConstantDistribution(),
) : IDDAlgorithm<U> {
    context(M)
    override suspend fun <M : Monad, T : U> minimize(
        items: List<T>,
        propertyTester: PropertyTester<M, T>,
    ): DDAlgorithmResult<T> {
        val buffer = ArrayDeque<T>()
        val probs = IdentityHashMap(initDist.on(items))
        val defaultProb = 1 - exp(-2.0 / items.size)
        items.forEach { item -> probs[item] = defaultProb }
        val currentItems = items.toMutableList()
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

    private fun <T : U> merge(
        probs: Map<U, Double>,
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
}

interface Distribution<U : DDItem> {
    fun on(items: List<U>): Map<U, Double>
}

class ConstantDistribution<U : DDItem> : Distribution<U> {
    override fun on(items: List<U>): Map<U, Double> {
        val defaultProb = 1 - exp(-2.0 / items.size)
        return items.associateWith { defaultProb }
    }
}
