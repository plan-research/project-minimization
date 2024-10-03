package org.plan.research.minimization.plugin.execution

import arrow.core.getOrElse
import arrow.core.raise.option
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.testFramework.utils.vfs.deleteRecursively
import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.core.model.PropertyTesterError
import org.plan.research.minimization.plugin.errors.SnapshotError
import org.plan.research.minimization.plugin.model.CompilationPropertyChecker
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.IJDDItem
import org.plan.research.minimization.plugin.model.ProjectFileDDItem
import org.plan.research.minimization.plugin.services.SnapshotManagerService

/**
 * A property tester for Delta Debugging algorithm that leverages different compilation strategies
 */
class SameExceptionPropertyTester<T : IJDDItem> private constructor(
    rootProject: Project,
    private val compilationPropertyChecker: CompilationPropertyChecker,
    private val initialException: Throwable,
) : PropertyTester<IJDDContext, T> {
    private val snapshotManager = rootProject.service<SnapshotManagerService>()

    /**
     * Tests whether the context's project has the same compiler exception as the root one
     *
     * @param context The context containing the current level project to test.
     * @param items The list of virtual file items to be tested within the context.
     */
    override suspend fun test(context: IJDDContext, items: List<T>): PropertyTestResult<IJDDContext> =
        snapshotManager.transaction(context) { newContext ->
            if (context.currentLevel == null) return@transaction newContext

            val targetFiles = context.currentLevel.minus(items.toSet()).filterIsInstance<ProjectFileDDItem>()

            writeAction {
                targetFiles.forEach { item ->
                    item.getVirtualFile(newContext)?.deleteRecursively()
                }
            }

            val compilationResult = compilationPropertyChecker
                .checkCompilation(newContext.project)
                .getOrElse { raise(PropertyTesterError.NoProperty) }

            when (compilationResult) {
                initialException -> newContext
                else -> raise(PropertyTesterError.UnknownProperty)
            }
        }.mapLeft {
            when (it) {
                is SnapshotError.Aborted -> it.reason
                else -> PropertyTesterError.UnknownProperty
            }
        }

    companion object {
        suspend fun <T : IJDDItem> create(
            compilerPropertyChecker: CompilationPropertyChecker,
            project: Project
        ) = option {
            val initialException = compilerPropertyChecker.checkCompilation(project).getOrNone().bind()
            SameExceptionPropertyTester<T>(project, compilerPropertyChecker, initialException)
        }
    }
}
