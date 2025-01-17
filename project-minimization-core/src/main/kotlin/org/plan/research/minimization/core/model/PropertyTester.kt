package org.plan.research.minimization.core.model

import org.plan.research.minimization.core.model.graph.GraphCut

import arrow.core.Either

typealias PropertyTestResult = Either<PropertyTesterError, Unit>

/**
 * Interface representing a tester for properties within a delta debugging context.
 *
 * This interface defines the contract for a property tester that evaluates a list of items
 * within a given context.
 * The purpose of the tester is to determine whether the given items
 * satisfy a specific property relevant to the context of a delta debugging process.
 *
 * @param T The type of items being analyzed and manipulated in the delta debugging process.
 */
interface PropertyTester<M : Monad, T : DDItem> {
    context(M)
    suspend fun test(items: List<T>): PropertyTestResult
}

interface ReversedPropertyTester<M : Monad, T : DDItem> {
    context(M)
    suspend fun test(itemsToDelete: List<T>): PropertyTestResult
}

/**
 * An interface that tests a specific property of a graph using a given context and a graph cut.
 *
 * @param V The type of the vertices in the graph
 */
interface PropertyTesterWithGraph<M : Monad, V : DDItem> {
    context(M)
    suspend fun test(cutToLeave: GraphCut<V>): PropertyTestResult
}

/**
 * Interface defining a tester for evaluating a specific property in a graph,
 * based on a specified graph cut that could be deleted
 *
 * @param V The type of vertices in the graph
 */
interface ReversedPropertyTesterWithGraph<M : Monad, V : DDItem> {
    context(M)
    suspend fun test(cutToDelete: GraphCut<V>): PropertyTestResult
}

sealed interface PropertyTesterError {
    data object NoProperty : PropertyTesterError
    data object UnknownProperty : PropertyTesterError
}
