package org.plan.research.minimization.plugin.execution

import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.core.model.PropertyTesterError
import org.plan.research.minimization.plugin.errors.SnapshotError
import org.plan.research.minimization.plugin.logging.withLog
import org.plan.research.minimization.plugin.logging.withLog
import org.plan.research.minimization.plugin.logging.withStatistics
import org.plan.research.minimization.plugin.model.*
import org.plan.research.minimization.plugin.model.exception.CompilationException
import org.plan.research.minimization.plugin.model.exception.ExceptionComparator
import org.plan.research.minimization.plugin.services.SnapshotManagerService

import arrow.core.getOrElse
import arrow.core.raise.option
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import mu.KotlinLogging

private typealias Listeners<T> = List<SameExceptionPropertyTester.PropertyCheckingListener<T>>

/**
 * A property tester for Delta Debugging algorithm that leverages different compilation strategies
 */
class SameExceptionPropertyTester<T : IJDDItem> private constructor(
    rootProject: Project,
    private val buildExceptionProvider: BuildExceptionProvider,
    private val comparator: ExceptionComparator,
    private val lens: ProjectItemLens<T>,
    private val initialException: CompilationException,
    private val listeners: Listeners<T> = emptyList(),
) : PropertyTester<IJDDContext, T> {
    private val snapshotManager = rootProject.service<SnapshotManagerService>()

    /**
     * Tests whether the context's project has the same compiler exception as the root one
     *
     * @param context The context containing the current level project to test.
     * @param items The list of project file items to be tested within the context.
     */
    override suspend fun test(context: IJDDContext, items: List<T>): PropertyTestResult<IJDDContext> =
        snapshotManager.transaction(context) { newContext ->
            context.currentLevel ?: listeners.forEach { it.onEmptyLevel(context) }.let { return@transaction context }

            listeners.forEach { it.beforeFocus(newContext, items) }
            val focusedContext =
                lens.focusOn(items, newContext)  // Assume that `newContext` has the same `.currentLevel` as `context`
            listeners.forEach { it.onSuccessfulFocus(focusedContext) }

            val compilationResult = buildExceptionProvider
                .checkCompilation(focusedContext)
                .getOrElse {
                    listeners.forEach { it.onSuccessfulCompilation(focusedContext) }
                    raise(PropertyTesterError.NoProperty)
                }
            listeners.forEach { it.onFailedCompilation(focusedContext, compilationResult) }
            val compareResult = comparator.areEquals(initialException, compilationResult)
            listeners.forEach {
                it.onComparedExceptions(
                    focusedContext,
                    initialException,
                    compilationResult,
                    compareResult,
                )
            }
            if (compareResult) {
                focusedContext
            } else {
                raise(PropertyTesterError.UnknownProperty)
            }
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

        suspend fun <T : IJDDItem> create(
            compilerPropertyChecker: BuildExceptionProvider,
            exceptionComparator: ExceptionComparator,
            lens: ProjectItemLens<T>,
            context: IJDDContext,
            listeners: Listeners<T> = emptyList(),
        ) = option {
            val initialException = compilerPropertyChecker.checkCompilation(context).getOrNone().bind()
            SameExceptionPropertyTester<T>(
                context.originalProject,
                compilerPropertyChecker,
                exceptionComparator,
                lens,
                initialException,
                listeners,
            )
                .also { logger.debug { "Initial exception is $initialException" } }
                .withLog()
                .withStatistics()
        }
    }
}
