package org.plan.research.minimization.core.algorithm.dd

import org.plan.research.minimization.core.model.DDInfo
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.Monad
import org.plan.research.minimization.core.model.PropertyTester

private class DDAlgorithmWithZeroTesting(private val ddAlgorithm: DDAlgorithm) : DDAlgorithm {
    context(M)
    override suspend fun <M : Monad, T : DDItem> minimize(
        items: List<T>,
        propertyTester: PropertyTester<M, T>,
        info: DDInfo<T>,
    ): DDAlgorithmResult<T> {
        val result = ddAlgorithm.minimize(items, propertyTester, info)

        return result.retained.singleOrNull()?.let {
            propertyTester.test(emptyList(), result.retained).fold(
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
