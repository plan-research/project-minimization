package org.plan.research.minimization.plugin.model.context.impl

import org.plan.research.minimization.plugin.model.context.HeavyIJDDContext

import com.intellij.openapi.project.Project

class DefaultProjectContext(
    project: Project,
    originalProject: Project = project,
) : HeavyIJDDContext<DefaultProjectContext>(project, originalProject) {
    override fun copy(project: Project): DefaultProjectContext = DefaultProjectContext(project, originalProject)

    override fun getThis(): DefaultProjectContext = this
}
