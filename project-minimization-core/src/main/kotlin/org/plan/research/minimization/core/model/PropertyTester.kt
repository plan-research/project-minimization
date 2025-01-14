package org.plan.research.minimization.core.model

import org.plan.research.minimization.core.model.graph.Graph
import org.plan.research.minimization.core.model.graph.GraphCut
import org.plan.research.minimization.core.model.graph.GraphEdge

import arrow.core.Either

typealias PropertyTestResult<C> = Either<PropertyTesterError, C>

/**
 * Interface representing a tester for properties within a delta debugging context.
 *
 * This interface defines the contract for a property tester that evaluates a list of items
 * within a given context.
 * The purpose of the tester is to determine whether the given items
 * satisfy a specific property relevant to the context of a delta debugging process.
 *
 * @param C The type of context that provides information relevant to the delta debugging process.
 * @param T The type of items being analyzed and manipulated in the delta debugging process.
 */
interface PropertyTester<C : DDContext, T : DDItem> {
    suspend fun test(context: C, items: List<T>): PropertyTestResult<C>
}

/**
 * An interface that tests a specific property of a graph using a given context and a graph cut.
 *
 * @param V The type of the vertices in the graph
 * @param E The type of the edges in the graph
 * @param G The type of the graph.
 * Represents the structure to be
 *          tested.
 */
interface PropertyTesterWithGraph<C : DDContextWithLevel<C>, V : DDItem, E : GraphEdge<V>, G : Graph<V, E, G>> {
    suspend fun test(context: C, cut: GraphCut<V>): PropertyTestResult<C>
}

sealed interface PropertyTesterError {
    data object NoProperty : PropertyTesterError
    data object UnknownProperty : PropertyTesterError
}
