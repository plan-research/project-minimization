package org.plan.research.minimization.plugin.model.context.impl

import org.plan.research.minimization.plugin.model.context.HeavyIJDDContext
import org.plan.research.minimization.plugin.model.context.IJDDContextCloner

import com.intellij.openapi.project.Project

class DefaultProjectContext(
    project: Project,
    originalProject: Project = project,
) : HeavyIJDDContext<DefaultProjectContext>(project, originalProject) {
    override fun copy(project: Project): DefaultProjectContext = DefaultProjectContext(project, originalProject)

    override suspend fun clone(cloner: IJDDContextCloner): DefaultProjectContext? =
        cloner.cloneHeavy(this)
}
