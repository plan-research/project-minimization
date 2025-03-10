package org.plan.research.minimization.plugin.compilation.exception

import org.plan.research.minimization.plugin.compilation.transformer.ExceptionTransformer
import org.plan.research.minimization.plugin.context.IJDDContext

interface CompilationException {
    suspend fun apply(transformation: ExceptionTransformer, context: IJDDContext): CompilationException
}
