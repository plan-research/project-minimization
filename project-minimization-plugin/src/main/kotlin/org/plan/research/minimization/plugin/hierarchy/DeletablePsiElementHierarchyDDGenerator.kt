package org.plan.research.minimization.plugin.hierarchy

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HDDLevel
import org.plan.research.minimization.core.model.Monad
import org.plan.research.minimization.plugin.model.IJHierarchicalDDGenerator
import org.plan.research.minimization.plugin.model.IJPropertyTester
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.item.PsiStubDDItem
import org.plan.research.minimization.plugin.model.monad.SnapshotWithProgressMonad
import org.plan.research.minimization.plugin.model.monad.WithProgressMonadT
import org.plan.research.minimization.plugin.psi.CompressingPsiItemTrie.NextPsiDDItemInfo
import org.plan.research.minimization.plugin.psi.StubCompressingPsiTrie
import org.plan.research.minimization.plugin.psi.stub.KtStub

import arrow.core.None
import arrow.core.raise.option

import java.nio.file.Path

private typealias DeletableNextItemInfo = NextPsiDDItemInfo<PsiStubDDItem, KtStub>

class DeletablePsiElementHierarchyDDGenerator<C : IJDDContext>(
    private val propertyChecker: IJPropertyTester<C, PsiStubDDItem>,
    private val perFileTries: Map<Path, StubCompressingPsiTrie>,
) : IJHierarchicalDDGenerator<C, PsiStubDDItem> {
    private val cache: MutableMap<PsiStubDDItem, StubCompressingPsiTrie> = mutableMapOf()
    private val maximumTrieDepth = perFileTries.maxOf { (_, trie) -> trie.maxDepth }

    context(SnapshotWithProgressMonad<C>)
    override suspend fun generateFirstLevel() = option {
        nextStep(1)  // Initial step of the progress bar

        val firstLevelItems = perFileTries.values
            .cache()
            .map(DeletableNextItemInfo::item)

        HDDLevel(firstLevelItems, propertyChecker)
    }

    context(SnapshotWithProgressMonad<C>)
    override suspend fun generateNextLevel(minimizationResult: DDAlgorithmResult<PsiStubDDItem>) =
        option {
            val nextNodesInTrie = minimizationResult
                .retained
                .map { cache[it] ?: raise(None) }
                .cache()
            ensure(nextNodesInTrie.isNotEmpty())
            nextNodesInTrie.reportProgress()
            val nextItems = nextNodesInTrie.map(DeletableNextItemInfo::item)

            HDDLevel(nextItems, propertyChecker)
        }

    private fun Collection<StubCompressingPsiTrie>.cache() = flatMap { it.getNextItems() }
        .onEach { (trieNode, item) -> cache[item] = trieNode }

    context(WithProgressMonadT<M>)
    @Suppress("MAGIC_NUMBER")
    private fun <M : Monad> List<DeletableNextItemInfo>.reportProgress() {
        val currentLevel = first().depth  // should be equal across all nodes
        nextStep((100 * currentLevel) / maximumTrieDepth)
    }
}
