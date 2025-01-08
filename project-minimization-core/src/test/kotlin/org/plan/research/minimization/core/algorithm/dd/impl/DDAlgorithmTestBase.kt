package org.plan.research.minimization.core.algorithm.dd.impl

import arrow.core.raise.either
import arrow.core.raise.ensure
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.core.model.*
import kotlin.random.Random
import kotlin.test.assertContentEquals

abstract class DDAlgorithmTestBase {
    abstract fun createAlgorithm(): DDAlgorithm

    data object EmptyDDContext : DDContext
    data class SomeDDItem(val value: Int) : DDItem

    class SimpleTester(private val target: Set<SomeDDItem>) : PropertyTester<EmptyDDContext, SomeDDItem> {
        context(DDContextMonad<EmptyDDContext>)
        override suspend fun test(
            items: List<SomeDDItem>
        ): PropertyTestResult = either {
            ensure(items.count { it in target } == target.size) { PropertyTesterError.NoProperty }
        }
    }

    class ComplexTester(
        private val target: Set<SomeDDItem>,
        private val badItems: Set<SomeDDItem>,
    ) : PropertyTester<EmptyDDContext, SomeDDItem> {
        context(DDContextMonad<EmptyDDContext>)
        override suspend fun test(
            items: List<SomeDDItem>
        ): PropertyTestResult = either {
            val badCount = items.count { it in badItems }
            ensure(badCount == 0 || badCount == badItems.size) { PropertyTesterError.UnknownProperty }
            ensure(items.count { it in target } == target.size) { PropertyTesterError.NoProperty }
        }
    }

    private suspend fun simpleTestWithSize(
        algorithm: DDAlgorithm,
        size: Int, targetSize: Int,
        random: Random, shuffled: Boolean,
    ) {
        val items = List(size) { SomeDDItem(it) }
        val target = if (shuffled) {
            items.shuffled(random).take(targetSize)
        } else {
            val index = random.nextInt(size - targetSize)
            items.subList(index, index + targetSize)
        }

        val propertyTester = SimpleTester(target.toSet())
        val monad = DDContextMonad(EmptyDDContext)
        val result = monad.run { algorithm.minimize(items, propertyTester) }
        assertContentEquals(result.sortedBy { it.value }, target.sortedBy { it.value })
    }

    private suspend fun complexTestWithSize(
        algorithm: DDAlgorithm,
        size: Int, targetSize: Int, badSize: Int,
        random: Random, shuffled: Boolean,
    ) {
        val items = List(size) { SomeDDItem(it) }
        val (target, bad) = if (shuffled) {
            items.shuffled(random).let {
                it.take(targetSize) to it.drop(targetSize).take(badSize)
            }
        } else {
            val index = random.nextInt(size - targetSize)
            val badIndex = random.nextInt(size - targetSize - badSize)
            val target = items.subList(index, index + targetSize)
            val bad = items.filterIndexed { i, _ ->
                if (i < index) {
                    i >= badIndex && i < badIndex + badSize
                } else if (i >= index + targetSize) {
                    (i - targetSize) >= badIndex && (i - targetSize) < badIndex + badSize
                } else {
                    false
                }
            }
            target to bad
        }

        val propertyTester = ComplexTester(target.toSet(), bad.toSet())
        val monad = DDContextMonad(EmptyDDContext)
        val result = monad.run { algorithm.minimize(items, propertyTester) }

        if (result.size == targetSize) {
            assertContentEquals(result.sortedBy { it.value }, target.sortedBy { it.value })
        } else {
            assertContentEquals(result.sortedBy { it.value }, (target + bad).sortedBy { it.value })
        }
    }

    @Test
    fun simpleTest() {
        val algorithm = createAlgorithm()
        val random = Random(42)
        runBlocking {
            repeat(ITERATIONS) {
                val size = random.nextInt(10, MAX_SIZE)
                val targetSize = random.nextInt(1, size / 2)
                val shuffled = random.nextBoolean()
                simpleTestWithSize(algorithm, size, targetSize, random, shuffled)
            }
        }
    }

    @Test
    fun complexTest() {
        val algorithm = createAlgorithm()
        val random = Random(42)
        runBlocking {
            repeat(ITERATIONS) {
                val size = random.nextInt(10, MAX_SIZE)
                val targetSize = random.nextInt(1, size / 2)
                val badSize = random.nextInt(1, targetSize)
                val shuffled = random.nextBoolean()
                complexTestWithSize(algorithm, size, targetSize, badSize, random, shuffled)
            }
        }
    }

    companion object {
        private const val ITERATIONS = 10
        private const val MAX_SIZE = 10000
    }

}