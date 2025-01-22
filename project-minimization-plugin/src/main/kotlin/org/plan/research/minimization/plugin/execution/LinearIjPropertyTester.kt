package org.plan.research.minimization.plugin.execution

import org.plan.research.minimization.plugin.benchmark.BenchmarkSettings
import org.plan.research.minimization.plugin.logging.withLog
import org.plan.research.minimization.plugin.logging.withStatistics
import org.plan.research.minimization.plugin.model.BuildExceptionProvider
import org.plan.research.minimization.plugin.model.ProjectItemLens
import org.plan.research.minimization.plugin.model.context.IJDDContextBase
import org.plan.research.minimization.plugin.model.exception.CompilationException
import org.plan.research.minimization.plugin.model.exception.ExceptionComparator
import org.plan.research.minimization.plugin.model.item.IJDDItem

import arrow.core.raise.option
import com.intellij.openapi.project.Project
import mu.KotlinLogging

/**
 * A property tester for Delta Debugging algorithm that leverages different compilation strategies
 */
class LinearIjPropertyTester<C : IJDDContextBase<C>, T : IJDDItem> private constructor(
    rootProject: Project,
    buildExceptionProvider: BuildExceptionProvider,
    comparator: ExceptionComparator,
    lens: ProjectItemLens<C, T>,
    initialException: CompilationException,
    listeners: Listeners<T> = emptyList(),
) : AbstractSameExceptionPropertyTester<C, T>(
    rootProject,
    buildExceptionProvider,
    comparator,
    lens,
    initialException,
    listeners,
) {
    companion object {
        private val logger = KotlinLogging.logger {}

        suspend fun <C : IJDDContextBase<C>, T : IJDDItem> create(
            compilerPropertyChecker: BuildExceptionProvider,
            exceptionComparator: ExceptionComparator,
            lens: ProjectItemLens<C, T>,
            context: C,
            listeners: Listeners<T> = emptyList(),
        ) = option {
            val initialException = compilerPropertyChecker.checkCompilation(context).getOrNone().bind()
            LinearIjPropertyTester(
                context.originalProject,
                compilerPropertyChecker,
                exceptionComparator,
                lens,
                initialException,
                listeners,
            )
                .also { logger.debug { "Initial exception is $initialException" } }
                .withLog()
                .let { if (BenchmarkSettings.isBenchmarkingEnabled) it.withStatistics() else it }
        }
    }
}
