package org.plan.research.minimization.plugin.execution

import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.core.model.PropertyTesterError
import org.plan.research.minimization.plugin.benchmark.BenchmarkSettings
import org.plan.research.minimization.plugin.errors.SnapshotError
import org.plan.research.minimization.plugin.logging.withLog
import org.plan.research.minimization.plugin.logging.withStatistics
import org.plan.research.minimization.plugin.model.BuildExceptionProvider
import org.plan.research.minimization.plugin.model.IJPropertyTester
import org.plan.research.minimization.plugin.model.ProjectItemLens
import org.plan.research.minimization.plugin.model.context.IJDDContextBase
import org.plan.research.minimization.plugin.model.exception.CompilationException
import org.plan.research.minimization.plugin.model.exception.ExceptionComparator
import org.plan.research.minimization.plugin.model.item.IJDDItem
import org.plan.research.minimization.plugin.model.monad.IJDDContextMonad
import org.plan.research.minimization.plugin.model.monad.SnapshotMonad

import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.option
import mu.KotlinLogging

class SameExceptionPropertyTester<C : IJDDContextBase<C>, T : IJDDItem> private constructor(
    private val buildExceptionProvider: BuildExceptionProvider,
    private val comparator: ExceptionComparator,
    private val lens: ProjectItemLens<C, T>,
    private val initialException: CompilationException,
    private val listeners: Listeners<T> = emptyList(),
) : IJPropertyTester<C, T> {
    /**
     * Tests whether the context's project has the same compiler exception as the root one
     */
    context(SnapshotMonad<C>)
    override suspend fun test(retainedItems: List<T>, deletedItems: List<T>): PropertyTestResult =
        transaction {
            focus(deletedItems)
            val compilationResult = compile().bind()
            compareResult(compilationResult).bind()
        }.mapLeft {
            when (it) {
                is SnapshotError.Aborted -> it.reason
                else -> PropertyTesterError.UnknownProperty
            }
        }

    context(IJDDContextMonad<C>)
    private suspend fun compile() = either {
        buildExceptionProvider
            .checkCompilation(context)
            .getOrElse {
                listeners.forEach { it.onSuccessfulCompilation(context) }
                raise(PropertyTesterError.NoProperty)
            }
            .also { exception -> listeners.forEach { it.onFailedCompilation(context, exception) } }
    }

    context(IJDDContextMonad<C>)
    private suspend fun focus(deletedItems: List<T>) {
        listeners.forEach { it.beforeFocus(context, deletedItems) }
        lens.focusOn(deletedItems)
        listeners.forEach { it.onSuccessfulFocus(context) }
    }

    context(IJDDContextMonad<C>)
    private suspend fun compareResult(compilationResult: CompilationException) = either {
        val compareResult = comparator.areEquals(initialException, compilationResult)
        listeners.forEach {
            it.onComparedExceptions(
                context,
                initialException,
                compilationResult,
                compareResult,
            )
        }
        ensure(compareResult) { PropertyTesterError.UnknownProperty }
    }

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
            SameExceptionPropertyTester(
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
