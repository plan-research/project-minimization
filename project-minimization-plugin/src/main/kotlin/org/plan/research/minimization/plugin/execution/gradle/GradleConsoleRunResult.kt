package org.plan.research.minimization.plugin.execution.gradle

import org.plan.research.minimization.plugin.model.CompilationException

data class GradleConsoleRunResult(
    val exitCode: Int,
    val stdOut: String,
    val stdErr: String,
    val system: String,
): CompilationException