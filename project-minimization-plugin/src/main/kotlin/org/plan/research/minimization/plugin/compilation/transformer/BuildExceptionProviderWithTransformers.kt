package org.plan.research.minimization.plugin.compilation.transformer

import org.plan.research.minimization.plugin.compilation.BuildExceptionProvider
import org.plan.research.minimization.plugin.compilation.CompilationResult
import org.plan.research.minimization.plugin.compilation.exception.CompilationException
import org.plan.research.minimization.plugin.context.IJDDContext

class BuildExceptionProviderWithTransformers(
    private val buildExceptionProvider: BuildExceptionProvider,
    private val transformers: List<ExceptionTransformer>,
) : BuildExceptionProvider {
    override suspend fun checkCompilation(context: IJDDContext): CompilationResult = buildExceptionProvider
        .checkCompilation(context)
        .map { it.apply(transformers, context) }

    private suspend fun CompilationException.apply(transformations: List<ExceptionTransformer>, context: IJDDContext) =
        transformations.fold(this) { acc, it -> acc.apply(it, context) }
}

fun BuildExceptionProvider.withTransformers(transformers: List<ExceptionTransformer>) = when (this) {
    is BuildExceptionProviderWithTransformers -> this
    else -> BuildExceptionProviderWithTransformers(this, transformers)
}
