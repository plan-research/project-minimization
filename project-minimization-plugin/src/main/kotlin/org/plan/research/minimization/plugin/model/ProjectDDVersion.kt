package org.plan.research.minimization.plugin.model

import com.intellij.openapi.project.Project
import org.plan.research.minimization.core.model.DDVersion

data class ProjectDDVersion(val project: Project) : DDVersion
