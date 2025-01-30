package org.plan.research.minimization.scripts.logs.model

data class ThroughMinimizationStatistics(
    val before: Int,
    val after: Int,
) {
    internal fun stale() = copy(after = before)
    internal fun continueWith(newValue: Int) = copy(before = after, after = newValue)
}
