package org.plan.research.minimization.plugin.compilation.exception

import org.plan.research.minimization.plugin.compilation.transformer.ExceptionTransformer
import org.plan.research.minimization.plugin.context.IJDDContext

import kotlinx.serialization.Serializable

@Serializable
data class IdeaCompilationException(val kotlincExceptions: List<KotlincException>) : CompilationException {
    override suspend fun apply(transformation: ExceptionTransformer, context: IJDDContext): CompilationException =
        transformation.transform(this, context)
}
