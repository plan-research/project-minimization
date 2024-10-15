package org.plan.research.minimization.plugin.execution

import org.plan.research.minimization.plugin.execution.exception.KotlincException
import org.plan.research.minimization.plugin.model.CompilationException

data class IdeaCompilationException(val kotlincExceptions: List<KotlincException>): CompilationException