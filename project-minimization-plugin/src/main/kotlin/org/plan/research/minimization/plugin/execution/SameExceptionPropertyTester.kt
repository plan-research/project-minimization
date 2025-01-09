package org.plan.research.minimization.plugin.execution

import arrow.core.getOrElse
import arrow.core.raise.ensure
import arrow.core.raise.option
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import mu.KotlinLogging
import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.core.model.PropertyTesterError
import org.plan.research.minimization.plugin.errors.SnapshotError
import org.plan.research.minimization.plugin.logging.withLog
import org.plan.research.minimization.plugin.model.BuildExceptionProvider
import org.plan.research.minimization.plugin.model.IJPropertyTester
import org.plan.research.minimization.plugin.model.ProjectItemLens
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.context.IJDDContextMonad
import org.plan.research.minimization.plugin.model.exception.CompilationException
import org.plan.research.minimization.plugin.model.exception.ExceptionComparator
import org.plan.research.minimization.plugin.model.item.IJDDItem
import org.plan.research.minimization.plugin.services.SnapshotManagerService

private typealias Listeners<T> = List<SameExceptionPropertyTester.PropertyCheckingListener<T>>

/**
 * A property tester for Delta Debugging algorithm that leverages different compilation strategies
 */
class SameExceptionPropertyTester<C : IJDDContext, T : IJDDItem> private constructor(
    rootProject: Project,
    private val buildExceptionProvider: BuildExceptionProvider,
    private val comparator: ExceptionComparator,
    private val lens: ProjectItemLens<C, T>,
    private val initialException: CompilationException,
    private val listeners: Listeners<T> = emptyList(),
) : IJPropertyTester<C, T> {
    private val snapshotManager = rootProject.service<SnapshotManagerService>()

    /**
     * Tests whether the context's project has the same compiler exception as the root one
     *
     * @param items The list of project file items to be tested within the context.
     */
    context(IJDDContextMonad<C>)
    override suspend fun test(items: List<T>): PropertyTestResult =
        snapshotManager.transaction {
            context.currentLevel ?: listeners.forEach { it.onEmptyLevel(context) }.let { return@transaction }

            listeners.forEach { it.beforeFocus(context, items) }
            lens.focusOn(items)
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

        suspend fun <C : IJDDContext, T : IJDDItem> create(
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
        }
    }
}
