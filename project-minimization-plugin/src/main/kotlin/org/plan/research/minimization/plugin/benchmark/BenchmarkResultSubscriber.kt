package org.plan.research.minimization.plugin.benchmark

import org.plan.research.minimization.plugin.errors.MinimizationError

import com.intellij.openapi.project.Project

interface BenchmarkResultSubscriber {
    fun onSuccess(project: Project, config: BenchmarkProject) = Unit
    fun onFailure(error: MinimizationError, config: BenchmarkProject) = Unit
    fun onConfigCreationError() = Unit
    fun onException(throwable: Throwable, config: BenchmarkProject) = Unit
}