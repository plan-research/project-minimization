package org.plan.research.minimization.core.algorithm.dd

import org.plan.research.minimization.core.model.*

import org.jgrapht.Graph

data class DDGraphAlgorithmResult<T : DDItem, E>(val retained: GraphCut<T, E>, val deleted: GraphCut<T, E>)

interface DDGraphAlgorithm {
    context(M)
    suspend fun <M : Monad, T : DDItem, E> minimize(
        graph: Graph<T, E>,
        propertyTester: GraphPropertyTester<M, T, E>,
    ): DDGraphAlgorithmResult<T, E>
}
