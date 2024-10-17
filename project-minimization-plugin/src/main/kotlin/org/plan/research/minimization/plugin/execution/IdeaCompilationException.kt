package org.plan.research.minimization.plugin.execution

import org.plan.research.minimization.plugin.execution.exception.KotlincException
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.exception.CompilationException
import org.plan.research.minimization.plugin.model.exception.ExceptionTransformation

/**
 * @property kotlincExceptions
 */
data class IdeaCompilationException(val kotlincExceptions: List<KotlincException>) : CompilationException {
    override suspend fun apply(transformation: ExceptionTransformation, context: IJDDContext): CompilationException =
        transformation.transform(this, context)
}
