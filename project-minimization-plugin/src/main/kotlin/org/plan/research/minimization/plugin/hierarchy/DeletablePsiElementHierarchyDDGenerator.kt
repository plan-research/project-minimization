package org.plan.research.minimization.plugin.hierarchy

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HDDLevel
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDDGenerator
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.PsiDDItem
import org.plan.research.minimization.plugin.psi.CompressingPsiItemTrie

import arrow.core.None
import arrow.core.raise.option

import java.nio.file.Path

class DeletablePsiElementHierarchyDDGenerator(
    private val propertyChecker: PropertyTester<IJDDContext, PsiDDItem>,
    private val perFileTries: Map<Path, CompressingPsiItemTrie>,
) : HierarchicalDDGenerator<IJDDContext, PsiDDItem> {
    private val cache: MutableMap<PsiDDItem, CompressingPsiItemTrie> = mutableMapOf()
    override suspend fun generateFirstLevel(context: IJDDContext) = option {
        val firstLevelItems = perFileTries.values
            .cacheAndGetItems()
        HDDLevel(context.copy(currentLevel = firstLevelItems), firstLevelItems, propertyChecker)
    }

    override suspend fun generateNextLevel(minimizationResult: DDAlgorithmResult<IJDDContext, PsiDDItem>) =
        option {
            val (context, items) = minimizationResult
            val nextItems = items
                .map { cache[it] ?: raise(None) }
                .cacheAndGetItems()
            HDDLevel(context.copy(currentLevel = nextItems), nextItems, propertyChecker)
        }

    private fun Collection<CompressingPsiItemTrie>.cacheAndGetItems() = flatMap { it.getNextItems() }
        .onEach { (trieNode, item) -> cache[item] = trieNode }
        .map { it.item }
}
