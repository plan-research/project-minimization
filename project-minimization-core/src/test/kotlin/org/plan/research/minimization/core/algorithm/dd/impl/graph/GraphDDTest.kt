package org.plan.research.minimization.core.algorithm.dd.impl.graph

import org.plan.research.minimization.core.algorithm.dd.DDGraphAlgorithm
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HDDLevel
import org.plan.research.minimization.core.algorithm.dd.impl.ProbabilisticDD
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.Monad

class TestGraphLayerMonadT<M : Monad, T : DDItem>(monad: M) : GraphLayerMonadT<M, T>(monad) {
    override fun onNextLevel(level: HDDLevel<M, T>) {}
}

object TestGraphLayerMonadTProvider : GraphDD.GraphLayerMonadTProvider {
    context(M)
    override fun <M : Monad, T : DDItem> provide(): GraphLayerMonadT<M, T> =
        TestGraphLayerMonadT(this@M)
}

class GraphDDTest : DDGraphAlgorithmTestBase() {
    override fun getAlgorithm(): DDGraphAlgorithm =
        GraphDD(ProbabilisticDD(), TestGraphLayerMonadTProvider)
}
