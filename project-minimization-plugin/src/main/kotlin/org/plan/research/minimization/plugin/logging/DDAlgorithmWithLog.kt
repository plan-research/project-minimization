package org.plan.research.minimization.plugin.logging

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.model.DDContext
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.PropertyTester

import mu.KotlinLogging

class DDAlgorithmWithLog(
    private val innerDDAlgorithm: DDAlgorithm,
) : DDAlgorithm {
    private val generalLogger = KotlinLogging.logger {}

    override suspend fun <C : DDContext, T : DDItem> minimize(
        context: C, items: List<T>,
        propertyTester: PropertyTester<C, T>,
    ): DDAlgorithmResult<C, T> {
        val result: DDAlgorithmResult<C, T>

        generalLogger.info {
            "Start minimization algorithm \n" +
                "Context - ${context::class.simpleName}, \n" +
                "items - ${(items.firstOrNull() ?: NoSuchElementException())::class.simpleName} \n" +
                "propertyTester - ${propertyTester::class.simpleName}"
        }
        generalLogger.trace {
            "Context - $context \n" +
                "items - $items \n" +
                "propertyTester - $propertyTester"
        }

        try {
            result = innerDDAlgorithm.minimize(context, items, propertyTester)
        } catch (e: Throwable) {
            generalLogger.error { "DDAlgorithm ended up with error: ${e.message}" }
            throw e
        }

        statLogger.info {
            "Start: ${items.size}, " +
                "End: ${result.items.size}, " +
                "Ratio: ${result.items.size.toDouble() / items.size}"
        }
        generalLogger.info { "End minimization algorithm" }

        return result
    }
}

fun DDAlgorithm.withLog(): DDAlgorithm = DDAlgorithmWithLog(this)
