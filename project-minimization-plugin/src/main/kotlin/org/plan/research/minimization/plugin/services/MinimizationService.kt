package org.plan.research.minimization.plugin.services

import arrow.core.getOrElse
import arrow.core.raise.either
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.plan.research.minimization.plugin.errors.HierarchyBuildError

@Service(Service.Level.PROJECT)
class MinimizationService(val project: Project) {
    suspend fun minimizeProject(project: Project) = either {
        val hierarchicalDD = project.service<HierarchicalDDService>()

        val hierarchy = project
            .service<ProjectHierarchyProducerService>()
            .produce()
            .getOrElse { raise(MinimizationError.HierarchyFailed(it)) }
        hierarchicalDD.minimizeHierarchy(hierarchy)
    }

    sealed interface MinimizationError {
        data class HierarchyFailed(val error: HierarchyBuildError) : MinimizationError
    }
}