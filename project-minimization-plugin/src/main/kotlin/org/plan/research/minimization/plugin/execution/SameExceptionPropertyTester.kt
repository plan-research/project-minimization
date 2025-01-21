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
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.context.IJDDContextBase
import org.plan.research.minimization.plugin.model.exception.CompilationException
import org.plan.research.minimization.plugin.model.exception.ExceptionComparator
import org.plan.research.minimization.plugin.model.item.IJDDItem
import org.plan.research.minimization.plugin.model.monad.IJDDContextMonad
import org.plan.research.minimization.plugin.services.SnapshotManagerService

import arrow.core.getOrElse
import arrow.core.raise.ensure
import arrow.core.raise.option
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import mu.KotlinLogging

private typealias Listeners<T> = List<SameExceptionPropertyTester.PropertyCheckingListener<T>>

/**
 * A property tester for Delta Debugging algorithm that leverages different compilation strategies
 */
class SameExceptionPropertyTester<C : IJDDContextBase<C>, T : IJDDItem> private constructor(
    rootProject: Project,
    private val buildExceptionProvider: BuildExceptionProvider,
    private val comparator: ExceptionComparator,
    private val lens: ProjectItemLens<C, T>,
    private val initialException: CompilationException,
    private val listeners: Listeners<T> = emptyList(),
) : IJPropertyTester<C, T> {
    private val snapshotManager = rootProject.service<SnapshotManagerService>()

    context(IJDDContextMonad<C>)
    override suspend fun test(retainedItems: List<T>, deletedItems: List<T>): PropertyTestResult =
        snapshotManager.transaction {
            listeners.forEach { it.beforeFocus(context, deletedItems) }
            lens.focusOn(deletedItems)
            listeners.forEach { it.onSuccessfulFocus(context) }

            val compilationResult = buildExceptionProvider
                .checkCompilation(context)
                .getOrElse {
                    listeners.forEach { it.onSuccessfulCompilation(context) }
                    raise(PropertyTesterError.NoProperty)
                }
            listeners.forEach { it.onFailedCompilation(context, compilationResult) }
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
        }.mapLeft {
            when (it) {
                is SnapshotError.Aborted -> it.reason
                else -> PropertyTesterError.UnknownProperty
            }
        }

    interface PropertyCheckingListener<T : IJDDItem> {
        fun onEmptyLevel(context: IJDDContext) = Unit
        fun beforeFocus(context: IJDDContext, items: List<T>) = Unit
        fun onSuccessfulFocus(context: IJDDContext) = Unit
        fun onSuccessfulCompilation(context: IJDDContext) = Unit
        fun onFailedCompilation(context: IJDDContext, result: CompilationException) = Unit
        fun onComparedExceptions(
            context: IJDDContext,
            initialException: CompilationException,
            newException: CompilationException,
            result: Boolean,
        ) = Unit
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
