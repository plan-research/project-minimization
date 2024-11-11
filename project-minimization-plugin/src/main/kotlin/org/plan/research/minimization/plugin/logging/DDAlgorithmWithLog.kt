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
    private val logger = KotlinLogging.logger {}

    override suspend fun <C : DDContext, T : DDItem> minimize(
        context: C, items: List<T>,
        propertyTester: PropertyTester<C, T>,
    ): DDAlgorithmResult<C, T> {
        val result: DDAlgorithmResult<C, T>

        logger.info {
            "Start minimization algorithm \n" +
                "Context - $context, \n" +
                "propertyTester - $propertyTester"
        }
        if (logger.isTraceEnabled) {
            logger.trace {
                "items - $items"
            }
        } else {
            logger.info {
                "items - ${(items.firstOrNull() ?: NoSuchElementException())::class.simpleName} \n"
            }
        }
        statLogger.info { "DDAlgorithm started with size: ${items.size}" }

        try {
            result = innerDDAlgorithm.minimize(context, items, propertyTester)
        } catch (e: Throwable) {
            logger.error { "DDAlgorithm ended up with error: ${e.message}" }
            throw e
        }

        statLogger.info { "DDAlgorithm ended with size and ratio: ${result.items.size}, ${result.items.size.toDouble() / items.size}" }
        logger.info { "End minimization algorithm" }

        return result
    }

    override fun toString(): String = innerDDAlgorithm.toString()
}

fun DDAlgorithm.withLog(): DDAlgorithm = DDAlgorithmWithLog(this)
