package org.plan.research.minimization.plugin.compilation.transformer

import org.plan.research.minimization.plugin.compilation.exception.IdeaCompilationException
import org.plan.research.minimization.plugin.compilation.exception.KotlincException
import org.plan.research.minimization.plugin.context.IJDDContext

interface ExceptionTransformer {
    suspend fun transform(exception: IdeaCompilationException, context: IJDDContext): IdeaCompilationException =
        exception

    suspend fun transform(exception: KotlincException.GeneralKotlincException, context: IJDDContext): KotlincException.GeneralKotlincException =
        exception

    suspend fun transform(
        exception: KotlincException.GenericInternalCompilerException,
        context: IJDDContext,
    ): KotlincException.GenericInternalCompilerException =
        exception

    suspend fun transform(exception: KotlincException.BackendCompilerException, context: IJDDContext): KotlincException.BackendCompilerException =
        exception

    suspend fun transform(exception: KotlincException.KspException, context: IJDDContext) = exception
}
