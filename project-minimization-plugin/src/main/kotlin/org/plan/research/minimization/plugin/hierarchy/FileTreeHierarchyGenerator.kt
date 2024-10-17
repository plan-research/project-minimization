package org.plan.research.minimization.plugin.hierarchy

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.intellij.openapi.components.service
import com.intellij.openapi.project.guessProjectDir
import org.plan.research.minimization.plugin.errors.HierarchyBuildError
import org.plan.research.minimization.plugin.errors.HierarchyBuildError.NoExceptionFound
import org.plan.research.minimization.plugin.errors.HierarchyBuildError.NoRootFound
import org.plan.research.minimization.plugin.execution.SameExceptionPropertyTester
import org.plan.research.minimization.plugin.getExceptionComparator
import org.plan.research.minimization.plugin.getExceptionTransformations
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.ProjectFileDDItem
import org.plan.research.minimization.plugin.model.ProjectHierarchyProducer
import org.plan.research.minimization.plugin.services.BuildExceptionProviderService
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings

class FileTreeHierarchyGenerator : ProjectHierarchyProducer<ProjectFileDDItem> {
    override suspend fun produce(
        fromContext: IJDDContext
    ): Either<HierarchyBuildError, FileTreeHierarchicalDDGenerator> = either {
        val project = fromContext.originalProject
        ensureNotNull(project.guessProjectDir()) { NoRootFound }
        val compilerPropertyTester = project.service<BuildExceptionProviderService>()
        val settings = project.service<MinimizationPluginSettings>()
        val propertyTester = SameExceptionPropertyTester
            .create<ProjectFileDDItem>(
                compilerPropertyTester,
                settings.state.exceptionComparingStrategy.getExceptionComparator(),
                settings.state.minimizationTransformations.map { it.getExceptionTransformations() },
                fromContext,
            )
            .getOrElse { raise(NoExceptionFound) }
        FileTreeHierarchicalDDGenerator(propertyTester)
    }
}