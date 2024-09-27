package org.plan.research.minimization.plugin.execution

import arrow.core.getOrElse
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.option
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.core.model.PropertyTesterError
import org.plan.research.minimization.plugin.model.CompilationPropertyChecker
import org.plan.research.minimization.plugin.model.ProjectDDVersion
import org.plan.research.minimization.plugin.model.PsiDDItem
import org.plan.research.minimization.plugin.services.ProjectCloningService


class SameExceptionPropertyTester private constructor(
    private val compilationPropertyChecker: CompilationPropertyChecker,
    project: Project,
    private val initialException: Throwable,
) : PropertyTester<ProjectDDVersion, PsiDDItem> {

    private val projectCloner = project.service<ProjectCloningService>()

    override suspend fun test(version: ProjectDDVersion, items: List<PsiDDItem>): PropertyTestResult<ProjectDDVersion> {
        val project = version.project

        val clonedProject = projectCloner.clone(project, items.map(PsiDDItem::psi))
            ?: return PropertyTesterError.UnknownProperty.left()

        return either {
            val compilationResult = compilationPropertyChecker
                .checkCompilation(clonedProject)
                .getOrElse { raise(PropertyTesterError.NoProperty) }

            when (compilationResult) {
                initialException -> ProjectDDVersion(clonedProject)
                else -> raise(PropertyTesterError.UnknownProperty)
            }
        }.onLeft { ProjectManagerEx.getInstanceEx().closeAndDispose(clonedProject) }
    }

    companion object {
        suspend fun create(
            compilerPropertyChecker: CompilationPropertyChecker,
            project: Project
        ) = option {
            val initialException = compilerPropertyChecker.checkCompilation(project).getOrNone().bind()
            SameExceptionPropertyTester(compilerPropertyChecker, project, initialException)
        }
    }
}
