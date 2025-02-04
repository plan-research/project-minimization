package org.plan.research.minimization.plugin.algorithm.hierarchy

import org.plan.research.minimization.plugin.algorithm.HierarchyBuildError.NoExceptionFound
import org.plan.research.minimization.plugin.algorithm.ProjectHierarchyProducer
import org.plan.research.minimization.plugin.algorithm.ProjectHierarchyProducerResult
import org.plan.research.minimization.plugin.algorithm.SameExceptionPropertyTester
import org.plan.research.minimization.plugin.context.IJDDContextBase
import org.plan.research.minimization.plugin.logging.LoggingPropertyCheckingListener
import org.plan.research.minimization.plugin.modification.item.ProjectFileDDItem
import org.plan.research.minimization.plugin.modification.lenses.FileDeletingItemLens
import org.plan.research.minimization.plugin.services.BuildExceptionProviderService
import org.plan.research.minimization.plugin.services.MinimizationPluginSettings
import org.plan.research.minimization.plugin.util.getExceptionComparator

import arrow.core.getOrElse
import arrow.core.raise.either
import com.intellij.openapi.components.service

class FileTreeHierarchyGenerator<C : IJDDContextBase<C>> : ProjectHierarchyProducer<C, ProjectFileDDItem> {
    override suspend fun produce(
        context: C,
    ): ProjectHierarchyProducerResult<C, ProjectFileDDItem> = either {
        val project = context.originalProject
        val compilerPropertyTester = project.service<BuildExceptionProviderService>()
        val propertyTester = SameExceptionPropertyTester
            .create(
                compilerPropertyTester,
                project.service<MinimizationPluginSettings>().state
                    .exceptionComparingStrategy.getExceptionComparator(),
                FileDeletingItemLens(),
                context,
                listOfNotNull(LoggingPropertyCheckingListener.create("file-level")),
            )
            .getOrElse { raise(NoExceptionFound) }
        FileTreeHierarchicalDDGenerator(propertyTester)
    }
}
