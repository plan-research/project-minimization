package org.plan.research.minimization.core.algorithm.dd

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.Monad
import org.plan.research.minimization.core.model.PropertyTester

private class DDAlgorithmWithZeroTesting<T : DDItem>(
    private val ddAlgorithm: DDAlgorithm<T>
) : DDAlgorithm<T> {
    context(M)
    override suspend fun <M : Monad> minimize(
        items: List<T>,
        propertyTester: PropertyTester<M, T>,
    ): DDAlgorithmResult<T> {
        val result = ddAlgorithm.minimize(items, propertyTester)

        return result.retained.singleOrNull()?.let {
            propertyTester.test(emptyList(), result.retained).fold(
                ifLeft = { result },
                ifRight = { DDAlgorithmResult(emptyList(), items) },
            )
        } ?: result
    }
}

fun <T : DDItem> DDAlgorithm<T>.withZeroTesting(): DDAlgorithm<T> = when (this) {
    is DDAlgorithmWithZeroTesting -> this
    else -> DDAlgorithmWithZeroTesting(this)
}
