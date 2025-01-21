package org.plan.research.minimization.core.model

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
    suspend fun test(retainedItems: List<T>, deletedItems: List<T>): PropertyTestResult
}

interface GraphPropertyTester<M : Monad, T : DDItem, E> {
    context(M)
    suspend fun test(retainedCut: GraphCut<T, E>, deletedCut: GraphCut<T, E>): PropertyTestResult
}

sealed interface PropertyTesterError {
    data object NoProperty : PropertyTesterError
    data object UnknownProperty : PropertyTesterError
}
