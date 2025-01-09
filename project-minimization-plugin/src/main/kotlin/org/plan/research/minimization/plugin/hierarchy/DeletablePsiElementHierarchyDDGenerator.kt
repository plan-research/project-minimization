package org.plan.research.minimization.plugin.hierarchy

import arrow.core.None
import arrow.core.raise.option
import com.intellij.platform.util.progress.SequentialProgressReporter
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HDDLevel
import org.plan.research.minimization.plugin.model.IJHierarchicalDDGenerator
import org.plan.research.minimization.plugin.model.IJPropertyTester
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.context.IJDDContextMonad
import org.plan.research.minimization.plugin.model.item.PsiStubDDItem
import org.plan.research.minimization.plugin.psi.CompressingPsiItemTrie.NextPsiDDItemInfo
import org.plan.research.minimization.plugin.psi.StubCompressingPsiTrie
import org.plan.research.minimization.plugin.psi.stub.KtStub
import java.nio.file.Path

private typealias DeletableNextItemInfo = NextPsiDDItemInfo<PsiStubDDItem, KtStub>

class DeletablePsiElementHierarchyDDGenerator<C: IJDDContext>(
    private val propertyChecker: IJPropertyTester<C, PsiStubDDItem>,
    private val perFileTries: Map<Path, StubCompressingPsiTrie>,
) : IJHierarchicalDDGenerator<C, PsiStubDDItem> {
    private val cache: MutableMap<PsiStubDDItem, StubCompressingPsiTrie> = mutableMapOf()
    private val maximumTrieDepth = perFileTries.maxOf { (_, trie) -> trie.maxDepth }

    context(IJDDContextMonad<C>)
    override suspend fun generateFirstLevel() = option {
        context.progressReporter?.nextStep(1)  // Initial step of the progress bar

        val firstLevelItems = perFileTries.values
            .cache()
            .map(DeletableNextItemInfo::item)

        updateContext {
            @Suppress("UNCHECKED_CAST")
            it.copy(currentLevel = firstLevelItems) as C
        }
        HDDLevel(firstLevelItems, propertyChecker)
    }

    context(IJDDContextMonad<C>)
    override suspend fun generateNextLevel(minimizationResult: DDAlgorithmResult<PsiStubDDItem>) =
        option {
            val nextNodesInTrie = minimizationResult
                .map { cache[it] ?: raise(None) }
                .cache()
            ensure(nextNodesInTrie.isNotEmpty())
            context.progressReporter?.let { nextNodesInTrie.reportProgress(it) }
            val nextItems = nextNodesInTrie.map(DeletableNextItemInfo::item)

            updateContext {
                @Suppress("UNCHECKED_CAST")
                it.copy(currentLevel = nextItems) as C
            }
            HDDLevel(nextItems, propertyChecker)
        }

    private fun Collection<StubCompressingPsiTrie>.cache() = flatMap { it.getNextItems() }
        .onEach { (trieNode, item) -> cache[item] = trieNode }

    @Suppress("MAGIC_NUMBER")
    private fun List<DeletableNextItemInfo>.reportProgress(reporter: SequentialProgressReporter) {
        val currentLevel = first().depth  // should be equal across all nodes
        reporter.nextStep((100 * currentLevel) / maximumTrieDepth)
    }
}
