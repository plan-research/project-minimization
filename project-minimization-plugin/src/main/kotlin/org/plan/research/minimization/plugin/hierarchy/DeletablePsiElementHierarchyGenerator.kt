package org.plan.research.minimization.plugin.hierarchy

import org.plan.research.minimization.plugin.errors.HierarchyBuildError.NoExceptionFound
import org.plan.research.minimization.plugin.errors.HierarchyBuildError.NoRootFound
import org.plan.research.minimization.plugin.execution.SameExceptionPropertyTester
import org.plan.research.minimization.plugin.getExceptionComparator
import org.plan.research.minimization.plugin.lenses.FileDeletingItemLens
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.ProjectHierarchyProducer
import org.plan.research.minimization.plugin.model.ProjectHierarchyProducerResult
import org.plan.research.minimization.plugin.model.PsiDDItem
import org.plan.research.minimization.plugin.psi.CompressingPsiItemTrie
import org.plan.research.minimization.plugin.services.BuildExceptionProviderService
import org.plan.research.minimization.plugin.services.MinimizationPluginSettings
import org.plan.research.minimization.plugin.services.MinimizationPsiManager

import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.intellij.openapi.components.service
import com.intellij.openapi.project.guessProjectDir

import java.nio.file.Path

class DeletablePsiElementHierarchyGenerator : ProjectHierarchyProducer<PsiDDItem> {
    override suspend fun produce(fromContext: IJDDContext): ProjectHierarchyProducerResult<PsiDDItem> = either {
        val project = fromContext.originalProject
        ensureNotNull(project.guessProjectDir()) { NoRootFound }

        val settings = project.service<MinimizationPluginSettings>()
        val propertyTester = SameExceptionPropertyTester
            .create<PsiDDItem>(
                project.service<BuildExceptionProviderService>(),
                settings.state.exceptionComparingStrategy.getExceptionComparator(),
                FileDeletingItemLens(),
                fromContext,
            )
            .getOrElse { raise(NoExceptionFound) }
        DeletablePsiElementHierarchyDDGenerator(propertyTester, buildTries(fromContext))
    }

    private suspend fun buildTries(context: IJDDContext): Map<Path, CompressingPsiItemTrie> {
        val items = service<MinimizationPsiManager>().findDeletablePsiItems(context)
        return items.groupBy(PsiDDItem::localPath)
            .mapValues { (_, items) -> CompressingPsiItemTrie.create(items) }
    }
}
