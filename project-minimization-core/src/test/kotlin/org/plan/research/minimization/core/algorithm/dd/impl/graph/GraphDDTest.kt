package org.plan.research.minimization.core.algorithm.dd.impl.graph

import org.plan.research.minimization.core.algorithm.dd.DDGraphAlgorithm
import org.plan.research.minimization.core.algorithm.dd.impl.ProbabilisticDD
import org.plan.research.minimization.core.model.*

object TestGraphLayerMonadTProvider : GraphDD.GraphLayerMonadTProvider {
    context(M)
    override suspend fun <M : Monad> runUnderProgress(block: WithProgressMonadFAsync<M, Unit>) {
        withEmptyProgress(block)
    }
}

class GraphDDTest : DDGraphAlgorithmTestBase() {
    override fun getAlgorithm(): DDGraphAlgorithm =
        GraphDD(ProbabilisticDD(), TestGraphLayerMonadTProvider)
}
