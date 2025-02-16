package org.plan.research.minimization.core.algorithm.dd.impl

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Provide
import net.jqwik.api.domains.DomainContextBase
import org.plan.research.minimization.core.algorithm.dd.impl.DDAlgorithmTestBase.SomeDDItem

class DDDomain : DomainContextBase() {
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
}

data class TestCase(
    val items: List<SomeDDItem>,
    val target: List<SomeDDItem>,
)