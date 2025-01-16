package org.plan.research.minimization.plugin.execution

import org.plan.research.minimization.plugin.apply
import org.plan.research.minimization.plugin.model.BuildExceptionProvider
import org.plan.research.minimization.plugin.model.CompilationResult
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.exception.ExceptionTransformation

class BuildExceptionProviderWithTransformations(
    private val buildExceptionProvider: BuildExceptionProvider,
    private val transformations: List<ExceptionTransformation>,
) : BuildExceptionProvider {
    override suspend fun checkCompilation(context: IJDDContext): CompilationResult = buildExceptionProvider
        .checkCompilation(context)
        .map { it.apply(transformations, context) }
}

fun BuildExceptionProvider.withTransformations(transformations: List<ExceptionTransformation>) = when (this) {
    is BuildExceptionProviderWithTransformations -> this
    else -> BuildExceptionProviderWithTransformations(this, transformations)
}
