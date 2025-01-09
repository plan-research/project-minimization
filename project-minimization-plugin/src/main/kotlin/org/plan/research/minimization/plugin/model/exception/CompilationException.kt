package org.plan.research.minimization.plugin.model.exception

import org.plan.research.minimization.plugin.model.context.IJDDContext

interface CompilationException {
    suspend fun apply(transformation: ExceptionTransformation, context: IJDDContext): CompilationException
}
