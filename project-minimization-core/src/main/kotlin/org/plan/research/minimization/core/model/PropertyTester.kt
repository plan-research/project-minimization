package org.plan.research.minimization.core.model

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

sealed interface PropertyTesterError {
    data object NoProperty : PropertyTesterError
    data object UnknownProperty : PropertyTesterError
}
