package org.plan.research.minimization.plugin.hierarchy.graph

import org.plan.research.minimization.plugin.errors.HierarchyBuildError.NoExceptionFound
import org.plan.research.minimization.plugin.errors.HierarchyBuildError.NoRootFound
import org.plan.research.minimization.plugin.execution.GraphIjPropertyTester
import org.plan.research.minimization.plugin.execution.comparable.withLogging
import org.plan.research.minimization.plugin.getExceptionComparator
import org.plan.research.minimization.plugin.lenses.FunctionDeletingLens
import org.plan.research.minimization.plugin.logging.LoggingPropertyCheckingListener
import org.plan.research.minimization.plugin.model.context.IJDDContextBase
import org.plan.research.minimization.plugin.model.context.WithImportRefCounterContext
import org.plan.research.minimization.plugin.model.context.WithInstanceLevelGraphContext
import org.plan.research.minimization.plugin.services.BuildExceptionProviderService
import org.plan.research.minimization.plugin.services.MinimizationPluginSettings

import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.intellij.openapi.components.service
import com.intellij.openapi.project.guessProjectDir
import mu.KotlinLogging

class InstanceLevelLayerHierarchyBuilder<C> where C : WithInstanceLevelGraphContext<C>, C : WithImportRefCounterContext<C>, C : IJDDContextBase<C> {
    private val logger = KotlinLogging.logger { }
    suspend fun produce(fromContext: C) = either {
        val project = fromContext.originalProject
        ensureNotNull(project.guessProjectDir()) { NoRootFound }

        val settings = project.service<MinimizationPluginSettings>()
        val exceptionComparator = settings.state.exceptionComparingStrategy.getExceptionComparator()
        val propertyTester = GraphIjPropertyTester
            .create(
                project.service<BuildExceptionProviderService>(),
                exceptionComparator.withLogging(),
                FunctionDeletingLens(),
                fromContext,
                listOf(LoggingPropertyCheckingListener("instance-level")),
            )
            .getOrElse { raise(NoExceptionFound) }
        InstanceLevelLayerHierarchyProducer(propertyTester)
    }
}
