package org.plan.research.minimization.core.algorithm.dd.impl

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.core.model.DDItem

class ProbabilisticDDTest : DDAlgorithmTestBase() {
    override fun <T : DDItem> createAlgorithm(): DDAlgorithm<T> = ProbabilisticDD()
}