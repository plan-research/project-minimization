package org.plan.research.minimization.plugin.hierarchy

import org.plan.research.minimization.plugin.errors.HierarchyBuildError.NoExceptionFound
import org.plan.research.minimization.plugin.errors.HierarchyBuildError.NoRootFound
import org.plan.research.minimization.plugin.execution.SameExceptionPropertyTester
import org.plan.research.minimization.plugin.getExceptionComparator
import org.plan.research.minimization.plugin.lenses.FileDeletingItemLens
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.ProjectHierarchyProducer
import org.plan.research.minimization.plugin.model.ProjectHierarchyProducerResult
import org.plan.research.minimization.plugin.model.PsiStubDDItem
import org.plan.research.minimization.plugin.psi.CompressingPsiItemTrie
import org.plan.research.minimization.plugin.psi.StubCompressingPsiTrie
import org.plan.research.minimization.plugin.services.BuildExceptionProviderService
import org.plan.research.minimization.plugin.services.MinimizationPluginSettings
import org.plan.research.minimization.plugin.services.MinimizationPsiManagerService

import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.intellij.openapi.components.service
import com.intellij.openapi.project.guessProjectDir

import java.nio.file.Path

class DeletablePsiElementHierarchyGenerator : ProjectHierarchyProducer<PsiStubDDItem> {
    override suspend fun produce(fromContext: IJDDContext): ProjectHierarchyProducerResult<PsiStubDDItem> = either {
        val project = fromContext.originalProject
        ensureNotNull(project.guessProjectDir()) { NoRootFound }

        val settings = project.service<MinimizationPluginSettings>()
        val propertyTester = SameExceptionPropertyTester
            .create<PsiStubDDItem>(
                project.service<BuildExceptionProviderService>(),
                settings.state.exceptionComparingStrategy.getExceptionComparator(),
                FileDeletingItemLens(),
                fromContext,
            )
            .getOrElse { raise(NoExceptionFound) }
        DeletablePsiElementHierarchyDDGenerator(propertyTester, buildTries(fromContext))
    }

    private suspend fun buildTries(context: IJDDContext): Map<Path, StubCompressingPsiTrie> {
        val items = service<MinimizationPsiManagerService>().findDeletablePsiItems(context)
        return items.groupBy(PsiStubDDItem::localPath)
            .mapValues { (_, items) -> CompressingPsiItemTrie.create(items) }
    }
}
