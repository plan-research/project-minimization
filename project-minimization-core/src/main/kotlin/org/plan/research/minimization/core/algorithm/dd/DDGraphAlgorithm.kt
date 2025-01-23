package org.plan.research.minimization.core.algorithm.dd

import org.plan.research.minimization.core.model.*

import org.jgrapht.Graph

data class DDGraphAlgorithmResult<T : DDItem>(val retained: GraphCut<T>, val deleted: GraphCut<T>)

interface DDGraphAlgorithm {
    context(M)
    suspend fun <M : Monad, T : DDItem, E> minimize(
        graph: Graph<T, E>,
        propertyTester: GraphPropertyTester<M, T>,
    ): DDGraphAlgorithmResult<T>
}
