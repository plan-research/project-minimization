package org.plan.research.minimization.plugin.logging

import org.plan.research.minimization.core.model.DDContext
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.core.model.PropertyTester

import mu.KotlinLogging

class PropertyTesterWithLog<C : DDContext, T : DDItem>(
    private val innerTester: PropertyTester<C, T>,
) : PropertyTester<C, T> {
    private val logger = KotlinLogging.logger {}

    override suspend fun test(context: C, items: List<T>): PropertyTestResult<C> {
        logger.trace { "Property test number of items - ${items.size}" }
        logger.trace { "Property test items - $items" }
        statLogger.info { "Property Test started with size: ${items.size}" }
        val result = innerTester.test(context, items)
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

fun <C : DDContext, T : DDItem> PropertyTester<C, T>.withLog(): PropertyTester<C, T> =
    PropertyTesterWithLog(this)
