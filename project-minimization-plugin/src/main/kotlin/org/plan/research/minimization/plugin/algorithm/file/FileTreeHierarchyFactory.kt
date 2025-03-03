package org.plan.research.minimization.plugin.algorithm.file

import org.plan.research.minimization.plugin.algorithm.MinimizationError
import org.plan.research.minimization.plugin.algorithm.MinimizationError.NoExceptionFound
import org.plan.research.minimization.plugin.algorithm.adapters.IJHierarchicalDDGenerator
import org.plan.research.minimization.plugin.algorithm.tester.SameExceptionPropertyTester
import org.plan.research.minimization.plugin.context.IJDDContextBase
import org.plan.research.minimization.plugin.logging.LoggingPropertyCheckingListener
import org.plan.research.minimization.plugin.modification.item.ProjectFileDDItem
import org.plan.research.minimization.plugin.modification.lenses.FileDeletingItemLens
import org.plan.research.minimization.plugin.services.BuildExceptionProviderService
import org.plan.research.minimization.plugin.services.MinimizationPluginSettings
import org.plan.research.minimization.plugin.util.getExceptionComparator

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import com.intellij.openapi.components.service

typealias FileTreeHierarchyFactoryResult<C> = Either<MinimizationError, IJHierarchicalDDGenerator<C, ProjectFileDDItem>>

object FileTreeHierarchyFactory {
    suspend fun <C : IJDDContextBase<C>> createFromContext(
        context: C,
    ): FileTreeHierarchyFactoryResult<C> = either {
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
