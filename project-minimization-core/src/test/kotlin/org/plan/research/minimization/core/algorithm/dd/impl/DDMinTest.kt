package org.plan.research.minimization.core.algorithm.dd.impl

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.core.utils.withLog

class DDMinTest : DDAlgorithmTestBase() {
    override fun createAlgorithm(): DDAlgorithm = DDMin().withLog()
}
