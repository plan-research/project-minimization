package org.plan.research.minimization.plugin.execution

import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.raise.option
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.core.model.PropertyTesterError
import org.plan.research.minimization.plugin.model.CompilationPropertyChecker
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.VirtualFileDDItem


class SameExceptionPropertyTester private constructor(
    private val compilationPropertyChecker: CompilationPropertyChecker,
    private val initialException: Throwable,
) : PropertyTester<IJDDContext, VirtualFileDDItem> {
    override suspend fun test(context: IJDDContext, items: List<VirtualFileDDItem>): PropertyTestResult<IJDDContext> {
        val project = context.project

        return either {
            val compilationResult = compilationPropertyChecker
                .checkCompilation(project)
                .getOrElse { raise(PropertyTesterError.NoProperty) }

            when (compilationResult) {
                initialException -> IJDDContext(project)
                else -> raise(PropertyTesterError.UnknownProperty)
            }
        }.onLeft { ProjectManagerEx.getInstanceEx().closeAndDispose(project) }
    }

    companion object {
        suspend fun create(
            compilerPropertyChecker: CompilationPropertyChecker,
            project: Project
        ) = option {
            val initialException = compilerPropertyChecker.checkCompilation(project).getOrNone().bind()
            SameExceptionPropertyTester(compilerPropertyChecker, initialException)
        }
    }
}
