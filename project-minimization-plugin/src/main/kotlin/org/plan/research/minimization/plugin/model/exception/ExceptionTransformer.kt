package org.plan.research.minimization.plugin.model.exception

import arrow.core.Option
import arrow.core.toOption
import org.plan.research.minimization.plugin.execution.IdeaCompilationException
import org.plan.research.minimization.plugin.execution.exception.KotlincException
import org.plan.research.minimization.plugin.execution.exception.KotlincException.*

interface ExceptionTransformation {
    suspend fun transform(exception: CompilationException): Option<CompilationException> = exception.toOption()
    suspend fun transform(exception: IdeaCompilationException): Option<IdeaCompilationException>
    suspend fun transform(exception: KotlincException): Option<KotlincException> = when (exception) {
        is GeneralKotlincException -> transform(exception)
        is BackendCompilerException -> transform(exception)
        is GenericInternalCompilerException -> transform(exception)
    }
    suspend fun transform(exception: GeneralKotlincException): Option<GeneralKotlincException>
    suspend fun transform(exception: GenericInternalCompilerException): Option<GenericInternalCompilerException>
    suspend fun transform(exception: BackendCompilerException): Option<BackendCompilerException>
}