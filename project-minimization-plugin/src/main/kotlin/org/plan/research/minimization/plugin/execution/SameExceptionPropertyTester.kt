package org.plan.research.minimization.plugin.execution

import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.core.model.PropertyTesterError
import org.plan.research.minimization.plugin.apply
import org.plan.research.minimization.plugin.errors.SnapshotError
import org.plan.research.minimization.plugin.model.BuildExceptionProvider
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.IJDDItem
import org.plan.research.minimization.plugin.model.ProjectFileDDItem
import org.plan.research.minimization.plugin.model.exception.CompilationException
import org.plan.research.minimization.plugin.model.exception.ExceptionComparator
import org.plan.research.minimization.plugin.model.exception.ExceptionTransformation
import org.plan.research.minimization.plugin.services.SnapshotManagerService

import arrow.core.getOrElse
import arrow.core.raise.option
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * A property tester for Delta Debugging algorithm that leverages different compilation strategies
 *
 * @param T
 * @param rootProject
 * @param buildExceptionProvider
 * @param transformations
 * @param comparator
 * @param initialException
 */
class SameExceptionPropertyTester<T : IJDDItem> private constructor(
    rootProject: Project,
    private val buildExceptionProvider: BuildExceptionProvider,
    private val transformations: List<ExceptionTransformation>,
    private val comparator: ExceptionComparator,
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

            val targetFiles = context.currentLevel.minus(items.toSet()).filterIsInstance<ProjectFileDDItem>()

            writeAction {
                targetFiles.forEach { item ->
                    item.getVirtualFile(newContext)?.delete(this)
                }
            }

            val compilationResult = buildExceptionProvider
                .checkCompilation(newContext.project)
                .getOrElse { raise(PropertyTesterError.NoProperty) }

            if (comparator.areEquals(initialException, compilationResult.apply(transformations, newContext))) {
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
        suspend fun <T : IJDDItem> create(
            compilerPropertyChecker: BuildExceptionProvider,
            exceptionComparator: ExceptionComparator,
            transformations: List<ExceptionTransformation>,
            context: IJDDContext,
        ) = option {
            val initialException = compilerPropertyChecker.checkCompilation(context.originalProject).getOrNone().bind()
            SameExceptionPropertyTester<T>(
                context.originalProject,
                compilerPropertyChecker,
                transformations,
                exceptionComparator,
                initialException.apply(transformations, context.copy(project = context.originalProject)),
            )
        }
    }
}
