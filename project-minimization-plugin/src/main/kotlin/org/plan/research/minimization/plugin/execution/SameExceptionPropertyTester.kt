package org.plan.research.minimization.plugin.execution

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.none
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.option
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.plugin.model.CompilationPropertyChecker
import org.plan.research.minimization.plugin.model.PsiDDItem
import org.plan.research.minimization.plugin.services.ProjectCloningService


class SameExceptionPropertyTester private constructor(
    private val compilationPropertyChecker: CompilationPropertyChecker,
    project: Project,
    private val initialException: Throwable,
) :
    PropertyTester<PsiDDItem> {
    private val projectCloner = project.service<ProjectCloningService>()
    private var lastTestedSnapshot = none<Project>()
    override suspend fun test(items: List<PsiDDItem>): PropertyTestResult = either {
        val projects = items.map { it.psi.project }.toSet()
        ensure(projects.size == 1) { raise(Error.FromDifferentProjects) }
        
        val clonedProject = projectCloner
            .clone(projects.first(), items.map(PsiDDItem::psi))
            ?: raise(Error.CopyingFailed)
        lastTestedSnapshot = Option(clonedProject)
        
        val compilationResult = compilationPropertyChecker
            .checkCompilation(clonedProject)
            .getOrElse { raise(Error.CompilationFailed) }
        
        when (compilationResult) {
            initialException -> PropertyTestResult.PRESENT
            else -> PropertyTestResult.NOT_PRESENT
        }
            .also { ProjectManagerEx.getInstanceEx().closeAndDispose(clonedProject) }
    }.getOrElse { PropertyTestResult.UNKNOWN }
    
    fun getLastSnapshot(): Option<Project> = lastTestedSnapshot

    sealed interface Error {
        data object FromDifferentProjects : Error
        data object CompilationFailed : Error
        data object CopyingFailed : Error
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