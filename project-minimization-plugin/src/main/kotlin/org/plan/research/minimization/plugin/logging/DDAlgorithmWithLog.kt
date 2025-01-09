package org.plan.research.minimization.plugin.logging

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.model.DDContext
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.PropertyTester

import mu.KotlinLogging
import org.plan.research.minimization.core.model.DDContextMonad

class DDAlgorithmWithLog(
    private val innerDDAlgorithm: DDAlgorithm,
) : DDAlgorithm {
    private val logger = KotlinLogging.logger {}

    context(M)
    override suspend fun <M : DDContextMonad<C>, C : DDContext, T : DDItem> minimize(
        items: List<T>,
        propertyTester: PropertyTester<M, C, T>,
    ): DDAlgorithmResult<T> {
        val result: DDAlgorithmResult<T>

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
            result = innerDDAlgorithm.minimize(items, propertyTester)
        } catch (e: Throwable) {
            logger.error { "DDAlgorithm ended up with error: ${e.message}" }
            throw e
        }

        statLogger.info { "DDAlgorithm ended with size and ratio: ${result.size}, ${result.size.toDouble() / items.size}" }
        logger.info { "End minimization algorithm" }

        return result
    }

    override fun toString(): String = innerDDAlgorithm.toString()
}

fun DDAlgorithm.withLog(): DDAlgorithm = DDAlgorithmWithLog(this)
