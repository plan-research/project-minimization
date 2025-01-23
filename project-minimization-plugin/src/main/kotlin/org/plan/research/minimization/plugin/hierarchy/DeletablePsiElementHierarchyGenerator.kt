package org.plan.research.minimization.plugin.hierarchy

import org.plan.research.minimization.plugin.errors.HierarchyBuildError.NoExceptionFound
import org.plan.research.minimization.plugin.errors.HierarchyBuildError.NoRootFound
import org.plan.research.minimization.plugin.execution.LinearIjPropertyTester
import org.plan.research.minimization.plugin.execution.comparable.withLogging
import org.plan.research.minimization.plugin.getExceptionComparator
import org.plan.research.minimization.plugin.lenses.FunctionDeletingLens
import org.plan.research.minimization.plugin.logging.LoggingPropertyCheckingListener
import org.plan.research.minimization.plugin.model.ProjectHierarchyProducer
import org.plan.research.minimization.plugin.model.ProjectHierarchyProducerResult
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.context.IJDDContextBase
import org.plan.research.minimization.plugin.model.context.WithImportRefCounterContext
import org.plan.research.minimization.plugin.model.item.PsiStubDDItem
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

class DeletablePsiElementHierarchyGenerator<C>(private val depthThreshold: Int) :
    ProjectHierarchyProducer<C, PsiStubDDItem> where C : IJDDContextBase<C>, C : WithImportRefCounterContext<C> {
    private val logger = KotlinLogging.logger { }

    override suspend fun produce(
        context: C,
    ): ProjectHierarchyProducerResult<C, PsiStubDDItem> = either {
        val project = context.originalProject
        ensureNotNull(project.guessProjectDir()) { NoRootFound }

        val settings = project.service<MinimizationPluginSettings>()
        val exceptionComparator = settings.state.exceptionComparingStrategy.getExceptionComparator()
        val propertyTester = LinearIjPropertyTester
            .create(
                project.service<BuildExceptionProviderService>(),
                exceptionComparator.withLogging(),
                FunctionDeletingLens(),
                context,
                listOfNotNull(LoggingPropertyCheckingListener.create("instance-level")),
            )
            .getOrElse { raise(NoExceptionFound) }
        DeletablePsiElementHierarchyDDGenerator(propertyTester, buildTries(context))
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
