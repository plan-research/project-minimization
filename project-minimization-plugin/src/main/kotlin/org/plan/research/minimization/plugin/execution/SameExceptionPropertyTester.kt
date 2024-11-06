package org.plan.research.minimization.plugin.execution

import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.core.model.PropertyTesterError
import org.plan.research.minimization.plugin.errors.SnapshotError
import org.plan.research.minimization.plugin.logging.withLog
import org.plan.research.minimization.plugin.model.*
import org.plan.research.minimization.plugin.model.exception.CompilationException
import org.plan.research.minimization.plugin.model.exception.ExceptionComparator
import org.plan.research.minimization.plugin.services.SnapshotManagerService

import arrow.core.getOrElse
import arrow.core.raise.option
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import mu.KotlinLogging

/**
 * A property tester for Delta Debugging algorithm that leverages different compilation strategies
 */
class SameExceptionPropertyTester<T : IJDDItem> private constructor(
    rootProject: Project,
    private val buildExceptionProvider: BuildExceptionProvider,
    private val comparator: ExceptionComparator,
    private val lens: ProjectItemLens,
    private val initialException: CompilationException,
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
            context.currentLevel ?: return@transaction newContext

            lens.focusOn(items, newContext)  // Assume that `newContext` has the same `.currentLevel` as `context`

            val compilationResult = buildExceptionProvider
                .checkCompilation(newContext)
                .getOrElse { raise(PropertyTesterError.NoProperty) }

            if (comparator.areEquals(initialException, compilationResult)) {
                newContext
            } else {
                raise(PropertyTesterError.UnknownProperty)
            }
        }.mapLeft {
            when (it) {
                is SnapshotError.Aborted -> it.reason
                else -> PropertyTesterError.UnknownProperty
            }
        }

    companion object {
        private val logger = KotlinLogging.logger {}

        suspend fun <T : IJDDItem> create(
            compilerPropertyChecker: BuildExceptionProvider,
            exceptionComparator: ExceptionComparator,
            lens: ProjectItemLens,
            context: IJDDContext,
        ) = option {
            val initialException = compilerPropertyChecker.checkCompilation(context).getOrNone().bind()
            SameExceptionPropertyTester<T>(
                context.originalProject,
                compilerPropertyChecker,
                exceptionComparator,
                lens,
                initialException,
            )
                .also { logger.debug { "Initial exception is $initialException" } }
                .withLog()
        }
    }
}
