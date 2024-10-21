package org.plan.research.minimization.plugin.logging

import org.plan.research.minimization.core.model.DDContext
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.core.model.PropertyTester

class PropertyTesterWithLog<C : DDContext, T : DDItem>(
    private val innerTester: PropertyTester<C, T>
) : PropertyTester<C, T> {

    override suspend fun test(context: C, items: List<T>): PropertyTestResult<C> {

        Loggers.generalLogger.trace { "Property test number of items - ${items.size}" }
        Loggers.generalLogger.trace { "Property test items - $items" }
        val result = innerTester.test(context, items)
        result.fold(
            { error ->
                Loggers.statLogger.debug { "Property Test resulted with error: $error" }
            },
            { value ->
                Loggers.statLogger.debug { "Property Test succeeded with context: $value" }
            }
        )
        return result
    }
}

fun <C : DDContext, T : DDItem> PropertyTester<C, T>.withLog(): PropertyTester<C, T> =
    PropertyTesterWithLog(this)