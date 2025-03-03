package org.plan.research.minimization.plugin.algorithm.tester

import com.intellij.openapi.components.service
import org.plan.research.minimization.plugin.algorithm.adapters.IJGraphPropertyTesterAdapter
import org.plan.research.minimization.plugin.context.IJDDContextBase
import org.plan.research.minimization.plugin.logging.LoggingPropertyCheckingListener
import org.plan.research.minimization.plugin.modification.item.IJDDItem
import org.plan.research.minimization.plugin.modification.item.PsiStubDDItem
import org.plan.research.minimization.plugin.modification.lenses.ProjectItemLens
import org.plan.research.minimization.plugin.services.BuildExceptionProviderService
import org.plan.research.minimization.plugin.services.MinimizationPluginSettings
import org.plan.research.minimization.plugin.util.getExceptionComparator

object PropertyTesterFactory {
    suspend fun <C : IJDDContextBase<C>, T : IJDDItem> createPropertyTester(
        lens: ProjectItemLens<C, T>,
        context: C,
        stageName: String,
    ) = SameExceptionPropertyTester.create(
        context.originalProject.service<BuildExceptionProviderService>(),
        context.originalProject.service<MinimizationPluginSettings>().state
            .exceptionComparingStrategy
            .getExceptionComparator(),
        lens,
        context,
        listOfNotNull(LoggingPropertyCheckingListener.create(stageName)),
    )

    suspend fun <C : IJDDContextBase<C>> createGraphPropertyTester(
        lens: ProjectItemLens<C, PsiStubDDItem>,
        context: C,
        stageName: String,
    ) = IJGraphPropertyTesterAdapter.create(
        context.originalProject.service<BuildExceptionProviderService>(),
        context.originalProject.service<MinimizationPluginSettings>().state
            .exceptionComparingStrategy
            .getExceptionComparator(),
        lens,
        context,
        listOfNotNull(LoggingPropertyCheckingListener.create(stageName)),
    )
}
