package org.plan.research.minimization.core.model

import arrow.core.Either

typealias PropertyTestResult<C> = Either<PropertyTesterError, C>

interface PropertyTester<C : DDContext, T : DDItem> {
    suspend fun test(context: C, items: List<T>): PropertyTestResult<C>
}

sealed interface PropertyTesterError {
    data object NoProperty : PropertyTesterError
    data object UnknownProperty : PropertyTesterError
}
