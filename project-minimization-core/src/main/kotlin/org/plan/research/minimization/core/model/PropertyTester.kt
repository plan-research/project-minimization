package org.plan.research.minimization.core.model

interface PropertyTester<T: DDItem> {
    suspend fun test(items: List<T>): PropertyTestResult
}