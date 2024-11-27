package org.plan.research.minimization.plugin.hierarchy

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HDDLevel
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDDGenerator
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.PsiStubDDItem
import org.plan.research.minimization.plugin.psi.CompressingPsiItemTrie.NextPsiDDItemInfo
import org.plan.research.minimization.plugin.psi.StubCompressingPsiTrie
import org.plan.research.minimization.plugin.psi.stub.KtStub

import arrow.core.None
import arrow.core.raise.option
import com.intellij.platform.util.progress.SequentialProgressReporter

import java.nio.file.Path

private typealias DeletableNextItemInfo = NextPsiDDItemInfo<PsiStubDDItem, KtStub>

class DeletablePsiElementHierarchyDDGenerator(
    private val propertyChecker: PropertyTester<IJDDContext, PsiStubDDItem>,
    private val perFileTries: Map<Path, StubCompressingPsiTrie>,
) : HierarchicalDDGenerator<IJDDContext, PsiStubDDItem> {
    private val cache: MutableMap<PsiStubDDItem, StubCompressingPsiTrie> = mutableMapOf()
    private val maximumTrieDepth = perFileTries.maxOf { (_, trie) -> trie.maxDepth }
    override suspend fun generateFirstLevel(context: IJDDContext) = option {
        context.progressReporter?.nextStep(1)  // Initial step of the progress bar

        val firstLevelItems = perFileTries.values
            .cache()
            .map(DeletableNextItemInfo::item)
        HDDLevel(context.copy(currentLevel = firstLevelItems), firstLevelItems, propertyChecker)
    }

    override suspend fun generateNextLevel(minimizationResult: DDAlgorithmResult<IJDDContext, PsiStubDDItem>) =
        option {
            val (context, items) = minimizationResult
            val nextNodesInTrie = items
                .map { cache[it] ?: raise(None) }
                .cache()
            ensure(nextNodesInTrie.isNotEmpty())
            context.progressReporter?.let { nextNodesInTrie.reportProgress(it) }
            val nextItems = nextNodesInTrie.map(DeletableNextItemInfo::item)

            HDDLevel(context.copy(currentLevel = nextItems), nextItems, propertyChecker)
        }

    private fun Collection<StubCompressingPsiTrie>.cache() = flatMap { it.getNextItems() }
        .onEach { (trieNode, item) -> cache[item] = trieNode }

    @Suppress("MAGIC_NUMBER")
    private fun List<DeletableNextItemInfo>.reportProgress(reporter: SequentialProgressReporter) {
        val currentLevel = first().depth  // should be equal across all nodes
        reporter.nextStep((100 * currentLevel) / maximumTrieDepth)
    }
}
