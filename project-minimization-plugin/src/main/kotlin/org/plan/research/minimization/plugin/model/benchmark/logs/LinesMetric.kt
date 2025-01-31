package org.plan.research.minimization.plugin.model.benchmark.logs

data class LinesMetric(
    val totalLines: ThroughMinimizationStatistics,
    val blankLines: ThroughMinimizationStatistics,
    val commentLines: ThroughMinimizationStatistics,
    val codeLines: ThroughMinimizationStatistics,
) {
    internal fun stale() = copy(
        totalLines = totalLines.stale(),
        blankLines = blankLines.stale(),
        commentLines = commentLines.stale(),
        codeLines = codeLines.stale(),
    )
}
