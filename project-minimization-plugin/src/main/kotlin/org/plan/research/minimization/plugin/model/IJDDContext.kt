package org.plan.research.minimization.plugin.model

import com.intellij.openapi.project.Project
import org.plan.research.minimization.core.model.DDContext

data class IJDDContext(
    val project: Project,
    val currentLevel: List<VirtualFileDDItem>? = null,
) : DDContext
