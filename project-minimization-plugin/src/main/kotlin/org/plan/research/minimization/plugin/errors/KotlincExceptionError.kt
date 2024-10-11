package org.plan.research.minimization.plugin.errors

sealed interface KotlincExceptionError {
    data class FailedToBuildException(val cause: Exception) : KotlincExceptionError
    data object NoExceptionFound : KotlincExceptionError
}