package org.plan.research.minimization.core.algorithm.dd.impl

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm

class ProbabilisticDDTest : DDAlgorithmTestBase() {
    override fun createAlgorithm(): DDAlgorithm = ProbabilisticDD()
}