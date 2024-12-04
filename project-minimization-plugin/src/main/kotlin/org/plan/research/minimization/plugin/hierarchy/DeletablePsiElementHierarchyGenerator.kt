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
import mu.KotlinLogging
import org.plan.research.minimization.plugin.execution.comparable.withLogging

import java.nio.file.Path

class DeletablePsiElementHierarchyGenerator : ProjectHierarchyProducer<PsiStubDDItem> {
    private val logger = KotlinLogging.logger { }
    override suspend fun produce(fromContext: IJDDContext): ProjectHierarchyProducerResult<PsiStubDDItem> = either {
        val project = fromContext.originalProject
        ensureNotNull(project.guessProjectDir()) { NoRootFound }

        val settings = project.service<MinimizationPluginSettings>()
        val propertyTester = SameExceptionPropertyTester
            .create<PsiStubDDItem>(
                project.service<BuildExceptionProviderService>(),
                settings.state.exceptionComparingStrategy.getExceptionComparator().withLogging(),
                FileDeletingItemLens(),
                fromContext,
            )
            .getOrElse { raise(NoExceptionFound) }
        DeletablePsiElementHierarchyDDGenerator(propertyTester, buildTries(fromContext))
    }

    private suspend fun buildTries(context: IJDDContext): Map<Path, StubCompressingPsiTrie> {
        val items = service<MinimizationPsiManagerService>().findDeletablePsiItems(context)
        logger.debug { "Found ${items.size} deletable items " }
        return items.groupBy(PsiStubDDItem::localPath)
            .mapValues { (_, items) -> CompressingPsiItemTrie.create(items) }
            .also { logger.debug { "Created ${it.size} tries. The maximum trie depth is ${it.maxOfOrNull { (_, trie) -> trie.maxDepth }}" } }
    }
}
