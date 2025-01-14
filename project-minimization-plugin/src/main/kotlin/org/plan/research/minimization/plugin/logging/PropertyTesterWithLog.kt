package org.plan.research.minimization.plugin.logging

import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.plugin.model.IJPropertyTester
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.context.IJDDContextMonad
import org.plan.research.minimization.plugin.model.item.IJDDItem

import mu.KotlinLogging

class PropertyTesterWithLog<C : IJDDContext, T : IJDDItem>(
    private val innerTester: IJPropertyTester<C, T>,
) : IJPropertyTester<C, T> {
    private val logger = KotlinLogging.logger {}

    context(IJDDContextMonad<C>)
    override suspend fun test(itemsToDelete: List<T>): PropertyTestResult {
        logger.trace { "Property test number of items - ${itemsToDelete.size}" }
        logger.trace { "Property test items - $itemsToDelete" }
        statLogger.info { "Property Test started with size: ${itemsToDelete.size}" }
        val result = innerTester.test(itemsToDelete)
        result.fold({ error ->
            logger.debug { "Property Test resulted with error: $error" }
            statLogger.info { "Property Test result: $error" }
        },
            { value ->
                logger.info { "Property Test succeeded with context: $value" }
                statLogger.info { "Property Test result: success" }
            },
        )
        return result
    }

    override fun toString(): String = innerTester.toString()
}

fun <C : IJDDContext, T : IJDDItem> IJPropertyTester<C, T>.withLog(): IJPropertyTester<C, T> =
    PropertyTesterWithLog(this)
