package org.plan.research.minimization.plugin.benchmark

import kotlinx.serialization.Serializable

@Serializable
data class BenchmarkConfig(val projects: List<BenchmarkProject>)
