package org.plan.research.minimization.plugin.model.exception

import org.plan.research.minimization.plugin.execution.IdeaCompilationException
import org.plan.research.minimization.plugin.execution.exception.KotlincException.*
import org.plan.research.minimization.plugin.model.IJDDContext

interface ExceptionTransformation {
    suspend fun transform(exception: IdeaCompilationException, context: IJDDContext): IdeaCompilationException =
        exception

    suspend fun transform(exception: GeneralKotlincException, context: IJDDContext): GeneralKotlincException =
        exception

    suspend fun transform(
        exception: GenericInternalCompilerException,
        context: IJDDContext,
    ): GenericInternalCompilerException =
        exception

    suspend fun transform(exception: BackendCompilerException, context: IJDDContext): BackendCompilerException =
        exception
}
