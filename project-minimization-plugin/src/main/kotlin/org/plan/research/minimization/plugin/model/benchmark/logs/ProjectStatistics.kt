package org.plan.research.minimization.plugin.model.benchmark.logs

import kotlin.time.Duration

// Data class to hold parsed project statistics
data class ProjectStatistics(
    val projectName: String,
    val elapsed: Duration,
    val ktFiles: ThroughMinimizationStatistics,
    val lines: LinesMetric,
    val numberOfCompilations: Int,
    val stageMetrics: List<StageStatistics>,
)
