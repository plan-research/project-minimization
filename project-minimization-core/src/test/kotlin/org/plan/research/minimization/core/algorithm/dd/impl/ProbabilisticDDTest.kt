package org.plan.research.minimization.core.algorithm.dd.impl

import kotlinx.coroutines.runBlocking
import net.jqwik.api.*
import net.jqwik.api.statistics.NumberRangeHistogram
import net.jqwik.api.statistics.Statistics
import net.jqwik.api.statistics.StatisticsReport
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.core.model.*
import kotlin.random.Random
import kotlin.test.assertContentEquals

class ProbabilisticDDTest : DDAlgorithmTestBase() {
    override fun createAlgorithm(): DDAlgorithm = ProbabilisticDD()

    class CountingTester<M : Monad, T : DDItem>(
        private val internal: PropertyTester<M, T>,
    ) : PropertyTester<M, T> {
        private var count = 0

        fun getCount() = count

        fun resetCount() {
            count = 0
        }

        context(M)
        override suspend fun test(retainedItems: List<T>, deletedItems: List<T>) =
            internal.test(retainedItems, deletedItems).also { count++ }
    }

    data class TestCase(
        val items: List<SomeDDItem>,
        val target: List<SomeDDItem>,
    )

    @Provide
    fun testCase(): Arbitrary<TestCase> {
        val itemsA = Arbitraries.integers().between(1, 1000).map { n -> List(n) { SomeDDItem(it) } }
        return itemsA.flatMap { items ->
            Arbitraries.integers().between(1, items.size).map {
                val target = items.take(it)
                TestCase(items, target)
            }
        }
    }

    @Property
    @StatisticsReport(format = NumberRangeHistogram::class)
    fun infoHelps(@ForAll("testCase") case: TestCase, @ForAll seed: Long) {
        val random = Random(seed)
        val items = case.items.shuffled(random)

        val algorithm = createAlgorithm()
        val tester = CountingTester(SimpleTester(target = case.target.toSet()))

        val simpleResult = runBlocking {
            EmptyMonad.run {
                algorithm.minimize(items, tester)
            }
        }
        val simpleCount = tester.getCount()
        tester.resetCount()

        assertContentEquals(
            simpleResult.retained.sortedBy { it.value },
            case.target.sortedBy { it.value }
        )

        val infoResult = runBlocking {
            EmptyMonad.run {
                algorithm.minimize(
                    items, tester,
                    info = DDInfo.fromImportance { it in case.target }
                )
            }
        }
        val infoCount = tester.getCount()
        tester.resetCount()

        assertContentEquals(
            infoResult.retained.sortedBy { it.value },
            case.target.sortedBy { it.value }
        )

        assert(infoCount <= simpleCount)

        Statistics.label("Difference in runs count").collect(simpleCount - infoCount)
    }
}