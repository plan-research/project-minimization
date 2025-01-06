package org.plan.research.minimization.core.algorithm.dd

import org.plan.research.minimization.core.model.DDContext
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.PropertyTester

private class DDAlgorithmWithZeroTesting(private val ddAlgorithm: DDAlgorithm) : DDAlgorithm {
    override suspend fun <C : DDContext, T : DDItem> minimize(
        context: C,
        items: List<T>,
        propertyTester: PropertyTester<C, T>,
    ): DDAlgorithmResult<C, T> {
        val result = ddAlgorithm.minimize(context, items, propertyTester)

        return result.items.singleOrNull()?.let {
            propertyTester.test(result.context, emptyList()).fold(
                ifLeft = { result },
                ifRight = { updatedContext -> DDAlgorithmResult(updatedContext, emptyList()) },
            )
        } ?: result
    }
}

fun DDAlgorithm.withZeroTesting(): DDAlgorithm = when (this) {
    is DDAlgorithmWithZeroTesting -> this
    else -> DDAlgorithmWithZeroTesting(this)
}
