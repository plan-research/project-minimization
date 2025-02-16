package org.plan.research.minimization.core.algorithm.dd.impl

import kotlinx.coroutines.runBlocking
import net.jqwik.api.*
import net.jqwik.api.statistics.NumberRangeHistogram
import net.jqwik.api.statistics.Statistics
import net.jqwik.api.statistics.StatisticsReport
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.core.model.*
import kotlin.random.Random
import kotlin.test.assertContentEquals

class ProbabilisticDDTest : DDAlgorithmTestBase() {
    override fun createAlgorithm(): DDAlgorithm = ProbabilisticDD()
}