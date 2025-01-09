package org.plan.research.minimization.plugin.hierarchy

import org.plan.research.minimization.plugin.errors.HierarchyBuildError.NoExceptionFound
import org.plan.research.minimization.plugin.execution.SameExceptionPropertyTester
import org.plan.research.minimization.plugin.getExceptionComparator
import org.plan.research.minimization.plugin.lenses.FileDeletingItemLens
import org.plan.research.minimization.plugin.logging.LoggingPropertyCheckingListener
import org.plan.research.minimization.plugin.model.ProjectHierarchyProducer
import org.plan.research.minimization.plugin.model.ProjectHierarchyProducerResult
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.item.ProjectFileDDItem
import org.plan.research.minimization.plugin.services.BuildExceptionProviderService
import org.plan.research.minimization.plugin.services.MinimizationPluginSettings

import arrow.core.getOrElse
import arrow.core.raise.either
import com.intellij.openapi.components.service

class FileTreeHierarchyGenerator : ProjectHierarchyProducer<IJDDContext, ProjectFileDDItem> {
    override suspend fun <C : IJDDContext> produce(
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
