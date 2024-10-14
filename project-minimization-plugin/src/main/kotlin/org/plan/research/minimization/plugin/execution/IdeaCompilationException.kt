package org.plan.research.minimization.plugin.execution

import org.plan.research.minimization.plugin.execution.exception.KotlincException
import org.plan.research.minimization.plugin.model.exception.CompilationException
import org.plan.research.minimization.plugin.model.exception.ExceptionTransformation

data class IdeaCompilationException(val kotlincExceptions: List<KotlincException>): CompilationException {
    override suspend fun transformBy(transformation: ExceptionTransformation) = transformation.transform(this)
}