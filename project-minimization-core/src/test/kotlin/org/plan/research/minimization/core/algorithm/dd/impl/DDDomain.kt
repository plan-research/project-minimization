package org.plan.research.minimization.core.algorithm.dd.impl

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Provide
import net.jqwik.api.domains.DomainContextBase
import org.plan.research.minimization.core.algorithm.dd.impl.DDAlgorithmTestBase.SomeDDItem
import kotlin.test.assertContains
import kotlin.math.min

class DDDomain : DomainContextBase() {
    @Provide
    fun testCase(): Arbitrary<TestCase> {
        val itemsA = Arbitraries.integers().between(1, MAX_ITEMS).map { n -> List(n) { SomeDDItem(it) } }
        return itemsA.flatMap { items ->
            Arbitraries.integers().between(1, min(MAX_TARGET, items.size)).flatMap { nTarget ->
                val target = items.take(nTarget)
                Arbitraries.integers().between(0, target.size).map { nImportant ->
                    val important = target.take(nImportant)
                    TestCase(items, target, important)
                }
            }
        }
    }

    companion object {
        const val MAX_ITEMS = 100
        const val MAX_TARGET = 4
    }
}

data class TestCase(
    val items: List<SomeDDItem>,
    val target: List<SomeDDItem>,
    val important: List<SomeDDItem>,
) {
    init {
        target.forEach { assertContains(items, it) }
        important.forEach { assertContains(target, it) }
    }
}