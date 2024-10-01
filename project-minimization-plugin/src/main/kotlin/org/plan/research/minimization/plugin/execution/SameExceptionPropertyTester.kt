package org.plan.research.minimization.plugin.execution

import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.option
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.core.model.PropertyTesterError
import org.plan.research.minimization.plugin.model.CompilationPropertyChecker
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.IJDDItem
import org.plan.research.minimization.plugin.model.IJDDItem.VirtualFileDDItem
import org.plan.research.minimization.plugin.services.ProjectCloningService

/**
 * A property tester for Delta Debugging algorithm that leverages different compilation strategies
 */
class SameExceptionPropertyTester<T : IJDDItem> private constructor(
    rootProject: Project,
    private val compilationPropertyChecker: CompilationPropertyChecker,
    private val initialException: Throwable,
) : PropertyTester<IJDDContext, T> {
    private val cloningService = rootProject.service<ProjectCloningService>()

    /**
     * Tests whether the context's project has the same compiler exception as the root one
     * Closes and disposes the cloned project immediately after checking the compilation.
     *
     * @param context The context containing the current level project to test.
     * @param items The list of virtual file items to be tested within the context.
     * For the given level all children and parent nodes will be considered as taken
     */
    override suspend fun test(context: IJDDContext, items: List<T>): PropertyTestResult<IJDDContext> {
        val project = context.project

        return either {
            ensure(items.isNotEmpty()) { PropertyTesterError.NoProperty }
            val clonedProject = when (items.firstOrNull()) {
                null -> cloningService.clone(project, emptyList())
                is VirtualFileDDItem ->
                    cloningService
                        .clone(project, items.filterIsInstance<VirtualFileDDItem>().map(VirtualFileDDItem::vfs))
                else -> TODO()
            }
                ?: raise(PropertyTesterError.UnknownProperty)


            val compilationResult = compilationPropertyChecker
                .checkCompilation(clonedProject)
                .also { ProjectManagerEx.getInstanceEx().closeAndDispose(clonedProject) } // Close immediately
                .getOrElse { raise(PropertyTesterError.NoProperty) }

            when (compilationResult) {
                initialException -> IJDDContext(project)
                else -> raise(PropertyTesterError.UnknownProperty)
            }
        }
    }

    companion object {
        suspend fun<T: IJDDItem> create(
            compilerPropertyChecker: CompilationPropertyChecker,
            project: Project
        ) = option {
            val initialException = compilerPropertyChecker.checkCompilation(project).getOrNone().bind()
            SameExceptionPropertyTester<T>(project, compilerPropertyChecker, initialException)
        }
    }
}
