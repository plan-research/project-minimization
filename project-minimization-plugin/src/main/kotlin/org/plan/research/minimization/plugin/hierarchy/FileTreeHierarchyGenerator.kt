package org.plan.research.minimization.plugin.hierarchy

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.intellij.openapi.components.service
import com.intellij.openapi.project.guessProjectDir
import org.plan.research.minimization.plugin.errors.HierarchyBuildError
import org.plan.research.minimization.plugin.errors.NoExceptionFound
import org.plan.research.minimization.plugin.errors.NoRootFound
import org.plan.research.minimization.plugin.execution.SameExceptionPropertyTester
import org.plan.research.minimization.plugin.model.dd.IJDDContext
import org.plan.research.minimization.plugin.model.dd.IJDDItem.VirtualFileDDItem
import org.plan.research.minimization.plugin.model.dd.ProjectHierarchyProducer
import org.plan.research.minimization.plugin.services.CompilationPropertyCheckerService
import org.plan.research.minimization.plugin.snapshot.VirtualFileProjectModifier

class FileTreeHierarchyGenerator : ProjectHierarchyProducer<VirtualFileDDItem> {
    override suspend fun produce(
        from: IJDDContext
    ): Either<HierarchyBuildError, FileTreeHierarchicalDDGenerator> = either {
        val project = from.snapshot.project
        ensureNotNull(project.guessProjectDir()) { NoRootFound }
        val compilerPropertyTester = project.service<CompilationPropertyCheckerService>()
        val propertyTester = SameExceptionPropertyTester.create<VirtualFileDDItem>(
            compilerPropertyTester,
            project,
            project.service<VirtualFileProjectModifier>(),
        )
            .getOrElse { raise(NoExceptionFound) }
        FileTreeHierarchicalDDGenerator(project, propertyTester)
    }
}