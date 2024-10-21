package org.plan.research.minimization.core.algorithm.dd.impl

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm

class DDMinTest : DDAlgorithmTestBase() {
    override fun createAlgorithm(): DDAlgorithm = DDMin()
}
