package org.plan.research.minimization.plugin.model.benchmark.logs

data class ThroughMinimizationStatistics(
    val before: Int,
    val after: Int,
) {
    internal fun stale() = copy(after = before)
    internal fun continueWith(newValue: Int) = copy(before = after, after = newValue)
}
