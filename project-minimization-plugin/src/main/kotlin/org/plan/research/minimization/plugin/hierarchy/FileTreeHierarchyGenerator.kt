package org.plan.research.minimization.plugin.hierarchy

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import org.plan.research.minimization.plugin.errors.HierarchyBuildError
import org.plan.research.minimization.plugin.errors.HierarchyBuildError.NoExceptionFound
import org.plan.research.minimization.plugin.errors.HierarchyBuildError.NoRootFound
import org.plan.research.minimization.plugin.execution.SameExceptionPropertyTester
import org.plan.research.minimization.plugin.model.ProjectHierarchyProducer
import org.plan.research.minimization.plugin.model.VirtualFileDDItem
import org.plan.research.minimization.plugin.services.CompilationPropertyCheckerService

class FileTreeHierarchyGenerator : ProjectHierarchyProducer<VirtualFileDDItem> {
    override suspend fun produce(
        from: Project
    ): Either<HierarchyBuildError, FileTreeHierarchicalDDGenerator> = either {
        ensureNotNull(from.guessProjectDir()) { NoRootFound }
        val compilerPropertyTester = from.service<CompilationPropertyCheckerService>()
        val propertyTester = SameExceptionPropertyTester.create<VirtualFileDDItem>(compilerPropertyTester, from)
            .getOrElse { raise(NoExceptionFound) }
        FileTreeHierarchicalDDGenerator(propertyTester)
    }
}