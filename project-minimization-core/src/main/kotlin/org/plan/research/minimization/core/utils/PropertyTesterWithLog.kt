package org.plan.research.minimization.core.utils

import mu.KotlinLogging
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.DDContext
import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.core.model.PropertyTester

class PropertyTesterWithLog<C : DDContext, T : DDItem>(
    private val innerTester: PropertyTester<C, T>
) : PropertyTester<C, T> {
    private val statLogger = KotlinLogging.logger("STATISTICS")

    override suspend fun test(context: C, items: List<T>): PropertyTestResult<C> {

        val result = innerTester.test(context, items)
        result.fold(
            { error ->
                statLogger.debug { "Property Test resulted with error: $error" }
            },
            { value ->
                statLogger.debug { "Property Test succeeded with context: $value" }
            }
        )
        return result
    }
}

fun <C : DDContext, T : DDItem> PropertyTester<C, T>.withLog(): PropertyTester<C, T> =
    PropertyTesterWithLog(this)