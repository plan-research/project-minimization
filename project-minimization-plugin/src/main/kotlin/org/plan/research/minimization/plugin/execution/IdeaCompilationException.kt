package org.plan.research.minimization.plugin.execution

import com.intellij.build.events.BuildEvent
import org.plan.research.minimization.plugin.model.CompilationException

data class IdeaCompilationException(val buildErrors: List<BuildEvent>): CompilationException