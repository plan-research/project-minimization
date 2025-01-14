package org.plan.research.minimization.plugin.hierarchy.graph

import org.plan.research.minimization.plugin.errors.HierarchyBuildError.NoExceptionFound
import org.plan.research.minimization.plugin.errors.HierarchyBuildError.NoRootFound
import org.plan.research.minimization.plugin.execution.DebugPropertyCheckingListener
import org.plan.research.minimization.plugin.execution.SameExceptionPropertyTester
import org.plan.research.minimization.plugin.execution.comparable.withLogging
import org.plan.research.minimization.plugin.getExceptionComparator
import org.plan.research.minimization.plugin.lenses.GraphFunctionDeletingLens
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.PsiStubDDItem
import org.plan.research.minimization.plugin.services.BuildExceptionProviderService
import org.plan.research.minimization.plugin.services.MinimizationPluginSettings

import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.intellij.openapi.components.service
import com.intellij.openapi.project.guessProjectDir
import mu.KotlinLogging

class InstanceLevelLayerHierarchyBuilder {
    private val logger = KotlinLogging.logger { }
    suspend fun produce(fromContext: IJDDContext) = either {
        val project = fromContext.originalProject
        ensureNotNull(project.guessProjectDir()) { NoRootFound }

        val settings = project.service<MinimizationPluginSettings>()
        val exceptionComparator = settings.state.exceptionComparingStrategy.getExceptionComparator()
        val propertyTester = SameExceptionPropertyTester
            .create<PsiStubDDItem>(
                project.service<BuildExceptionProviderService>(),
                exceptionComparator.withLogging(),
                GraphFunctionDeletingLens(),
                fromContext,
                listOf(DebugPropertyCheckingListener("instance-level")),
            )
            .getOrElse { raise(NoExceptionFound) }
        InstanceLevelLayerHierarchyProducer(propertyTester)
    }
}
