package org.plan.research.minimization.plugin.model.context.impl

import com.intellij.openapi.project.Project
import org.plan.research.minimization.plugin.model.context.HeavyIJDDContext

class DefaultProjectContext(
    project: Project,
    originalProject: Project = project,
) : HeavyIJDDContext<DefaultProjectContext>(project, originalProject) {
    override fun copy(project: Project): DefaultProjectContext = DefaultProjectContext(project, originalProject)
}
