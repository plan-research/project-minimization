package org.plan.research.minimization.plugin.hierarchy

import org.plan.research.minimization.plugin.errors.HierarchyBuildError.NoExceptionFound
import org.plan.research.minimization.plugin.errors.HierarchyBuildError.NoRootFound
import org.plan.research.minimization.plugin.execution.DebugPropertyCheckingListener
import org.plan.research.minimization.plugin.execution.SameExceptionPropertyTester
import org.plan.research.minimization.plugin.execution.comparable.withLogging
import org.plan.research.minimization.plugin.getExceptionComparator
import org.plan.research.minimization.plugin.lenses.FunctionDeletingLens
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

import java.nio.file.Path

class DeletablePsiElementHierarchyGenerator(private val depthThreshold: Int) : ProjectHierarchyProducer<PsiStubDDItem> {
    private val logger = KotlinLogging.logger { }
    override suspend fun produce(fromContext: IJDDContext): ProjectHierarchyProducerResult<PsiStubDDItem> = either {
        val project = fromContext.originalProject
        ensureNotNull(project.guessProjectDir()) { NoRootFound }

        val settings = project.service<MinimizationPluginSettings>()
        val exceptionComparator = settings.state.exceptionComparingStrategy.getExceptionComparator()
        val propertyTester = SameExceptionPropertyTester
            .create<PsiStubDDItem>(
                project.service<BuildExceptionProviderService>(),
                exceptionComparator.withLogging(),
                FunctionDeletingLens(),
                fromContext,
                listOfNotNull(DebugPropertyCheckingListener.create<PsiStubDDItem>("instance-level")),
            )
            .getOrElse { raise(NoExceptionFound) }
        DeletablePsiElementHierarchyDDGenerator(propertyTester, buildTries(fromContext))
    }

    private suspend fun buildTries(context: IJDDContext): Map<Path, StubCompressingPsiTrie> {
        val items = service<MinimizationPsiManagerService>().findDeletablePsiItems(context)
        logger.debug { "Found ${items.size} deletable items " }
        val filteredItems = items.asSequence().filter { it.childrenPath.size <= depthThreshold }
        return filteredItems.groupBy(PsiStubDDItem::localPath)
            .mapValues { (_, items) -> CompressingPsiItemTrie.create(items) }
            .also { logger.debug { "Created ${it.size} tries. The maximum trie depth is ${it.maxOfOrNull { (_, trie) -> trie.maxDepth }}" } }
    }
}
