package org.plan.research.minimization.plugin.execution.gradle

data class GradleConsoleRunResult(
    val exitCode: Int,
    val stdOut: String,
    val stdErr: String,
) {
    companion object {
        const val EXIT_CODE_FAIL = 1
        const val EXIT_CODE_OK = 0
    }
}
