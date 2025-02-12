package org.plan.research.minimization.core.algorithm.dd

import org.plan.research.minimization.core.model.*

import org.jgrapht.Graph

data class DDGraphAlgorithmResult<T : DDItem>(val retained: GraphCut<T>, val deleted: GraphCut<T>)

interface DDGraphAlgorithm<T : DDItem> {
    context(M)
    suspend fun <M : Monad, E> minimize(
        graph: Graph<T, E>,
        propertyTester: GraphPropertyTester<M, T>,
    ): DDGraphAlgorithmResult<T>
}
