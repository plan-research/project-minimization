package org.plan.research.minimization.core.algorithm.dd

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.Monad
import org.plan.research.minimization.core.model.PropertyTester

private class DDAlgorithmWithZeroTesting(private val ddAlgorithm: DDAlgorithm) : DDAlgorithm {
    context(M)
    override suspend fun <M : Monad, T : DDItem> minimize(
        items: List<T>,
        propertyTester: PropertyTester<M, T>,
    ): DDAlgorithmResult<T> {
        val result = ddAlgorithm.minimize(items, propertyTester)

        return result.survived.singleOrNull()?.let {
            propertyTester.test(emptyList(), result.survived).fold(
                ifLeft = { result },
                ifRight = { DDAlgorithmResult(emptyList(), items) },
            )
        } ?: result
    }
}

fun DDAlgorithm.withZeroTesting(): DDAlgorithm = when (this) {
    is DDAlgorithmWithZeroTesting -> this
    else -> DDAlgorithmWithZeroTesting(this)
}
