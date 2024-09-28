package org.plan.research.minimization.plugin.model

import com.intellij.openapi.project.Project
import org.plan.research.minimization.core.model.DDContext

// FIXME: In the current implementation the projects are not closed and disposed which leads to memory leaks.
//  Need to adjust the architecture to comprehend this.
//  E.g. fix the moment when the project is closed or replace project with `Path` and open the project each time it needed
data class IJDDContext(val project: Project) : DDContext
