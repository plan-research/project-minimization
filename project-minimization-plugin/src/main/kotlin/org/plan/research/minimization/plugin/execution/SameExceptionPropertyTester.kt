package org.plan.research.minimization.plugin.execution

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.option
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.core.model.PropertyTesterError
import org.plan.research.minimization.plugin.errors.SnapshotBuildingError
import org.plan.research.minimization.plugin.model.dd.CompilationPropertyChecker
import org.plan.research.minimization.plugin.model.dd.IJDDContext
import org.plan.research.minimization.plugin.model.dd.IJDDItem
import org.plan.research.minimization.plugin.model.snapshot.ProjectShrinkProducer
import org.plan.research.minimization.plugin.model.snapshot.SnapshotDecision
import org.plan.research.minimization.plugin.services.SnapshottingService

/**
 * A property tester for Delta Debugging algorithm that leverages different compilation strategies
 */
class SameExceptionPropertyTester<T : IJDDItem> private constructor(
    rootProject: Project,
    private val compilationPropertyChecker: CompilationPropertyChecker,
    private val projectShrinkProducer: ProjectShrinkProducer<T>,
    private val initialException: Throwable,
) : PropertyTester<IJDDContext, T> {
    private val snapshottingService = rootProject.service<SnapshottingService>()

    /**
     * Tests whether the context's project has the same compiler exception as the root one
     * Closes and disposes the cloned project immediately after checking the compilation.
     *
     * @param context The context containing the current level project to test.
     * @param items The list of virtual file items to be tested within the context.
     * For the given level all children and parent nodes will be considered as taken
     */
    override suspend fun test(context: IJDDContext, items: List<T>): PropertyTestResult<IJDDContext> {
        return either {
            var result: PropertyTestResult<IJDDContext> = PropertyTesterError.UnknownProperty.left()
            val deletingAction =
                projectShrinkProducer.modifyWith(context, items) ?: raise(PropertyTesterError.UnknownProperty)

            val transactionResult = snapshottingService
                .makeTransaction(context.snapshot) {
                    deletingAction(it)
                    result = either {
                        val exception = compilationPropertyChecker.checkCompilation(it)
                        ensure(exception is Either.Right) { PropertyTesterError.NoProperty }
                        when (exception.value) {
                            initialException -> context
                            else -> raise(PropertyTesterError.UnknownProperty)
                        }
                    }
                    SnapshotDecision.Rollback
                }
            ensure(transactionResult.leftOrNull() == SnapshotBuildingError.Aborted) { PropertyTesterError.UnknownProperty }
            result.bind()
        }
    }

    companion object {
        suspend fun <T : IJDDItem> create(
            compilerPropertyChecker: CompilationPropertyChecker,
            project: Project,
            projectModifier: ProjectShrinkProducer<T>
        ) = option {
            val initialException = compilerPropertyChecker.checkCompilation(project).getOrNone().bind()
            SameExceptionPropertyTester(project, compilerPropertyChecker, projectModifier, initialException)
        }
    }
}
