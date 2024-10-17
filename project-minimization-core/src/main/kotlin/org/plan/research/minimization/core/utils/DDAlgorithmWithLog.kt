package org.plan.research.minimization.core.utils

import mu.KotlinLogging
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.model.DDContext
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.PropertyTester

class DDAlgorithmWithLog (
    private val innerDDAlgorithm: DDAlgorithm
) : DDAlgorithm {
    private val statLogger = KotlinLogging.logger("STATISTICS")
    private val workingLogger = KotlinLogging.logger("WORKING")

    override suspend fun <C : DDContext, T : DDItem> minimize(
        context: C, items: List<T>,
        propertyTester: PropertyTester<C, T>
    ): DDAlgorithmResult<C, T> {

        workingLogger.info { "Start minimization algorithm" }

        val result: DDAlgorithmResult<C, T> = innerDDAlgorithm.minimize(context, items, propertyTester)

        statLogger.info { "Start: ${items.size}, " +
                "End: ${result.items.size}, " +
                "Ratio: ${result.items.size.toDouble() / items.size}" }
        workingLogger.info { "End minimization algorithm" }

        return result
    }
}

fun DDAlgorithm.withLog(): DDAlgorithm = DDAlgorithmWithLog(this)