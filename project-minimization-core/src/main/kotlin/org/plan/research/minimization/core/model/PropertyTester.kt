package org.plan.research.minimization.core.model

import arrow.core.Either

typealias PropertyTestResult<V> = Either<PropertyTesterError, V>

interface PropertyTester<V : DDVersion, T : DDItem> {
    suspend fun test(version: V, items: List<T>): PropertyTestResult<V>
}

sealed interface PropertyTesterError {
    data object NoProperty : PropertyTesterError
    data object UnknownProperty : PropertyTesterError
}
