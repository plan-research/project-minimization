package org.plan.research.minimization.plugin.hierarchy

import org.plan.research.minimization.plugin.errors.HierarchyBuildError
import org.plan.research.minimization.plugin.errors.HierarchyBuildError.NoExceptionFound
import org.plan.research.minimization.plugin.execution.SameExceptionPropertyTester
import org.plan.research.minimization.plugin.getExceptionComparator
import org.plan.research.minimization.plugin.lenses.FileDeletingItemLens
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.ProjectFileDDItem
import org.plan.research.minimization.plugin.model.ProjectHierarchyProducer
import org.plan.research.minimization.plugin.services.BuildExceptionProviderService
import org.plan.research.minimization.plugin.services.MinimizationPluginSettings

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import com.intellij.openapi.components.service

class FileTreeHierarchyGenerator : ProjectHierarchyProducer<ProjectFileDDItem> {
    override suspend fun produce(
        fromContext: IJDDContext,
    ): Either<HierarchyBuildError, FileTreeHierarchicalDDGenerator> = either {
        val project = fromContext.originalProject
        val compilerPropertyTester = project.service<BuildExceptionProviderService>()
        val propertyTester = SameExceptionPropertyTester
            .create<ProjectFileDDItem>(
                compilerPropertyTester,
                project.service<MinimizationPluginSettings>().state.state
                    .exceptionComparingStrategy.getExceptionComparator(),
                FileDeletingItemLens(),
                fromContext,
            )
            .getOrElse { raise(NoExceptionFound) }
        FileTreeHierarchicalDDGenerator(propertyTester)
    }
}
