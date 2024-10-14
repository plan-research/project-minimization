package org.plan.research.minimization.plugin.execution.gradle


data class GradleConsoleRunResult(
    val exitCode: Int,
    val stdOut: String,
    val stdErr: String,
    val system: String,
)