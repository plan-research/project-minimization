package org.plan.research.minimization.plugin.execution.gradle

/**
 * @property exitCode
 * @property stdOut
 * @property stdErr
 * @property system
 */
data class GradleConsoleRunResult(
    val exitCode: Int,
    val stdOut: String,
    val stdErr: String,
    val system: String,
)
