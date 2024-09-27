package org.plan.research.minimization.plugin.hierarchy

import arrow.core.raise.option
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HDDLevel
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDDGenerator
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.PsiDDItem

abstract class AbstractIJHierarchicalDDGenerator(
    protected val propertyTester: PropertyTester<IJDDContext, PsiDDItem>
) : HierarchicalDDGenerator<IJDDContext, PsiDDItem> {

    override suspend fun generateNextLevel(
        minimizationResult: DDAlgorithmResult<IJDDContext, PsiDDItem>
    ): HDDLevel<IJDDContext, PsiDDItem>? = option {
        val nextLevel = minimizationResult.items.flatMap { it.psi.children.toList() }.map { PsiDDItem(it) }
        ensure(nextLevel.isNotEmpty())
        HDDLevel(minimizationResult.context, nextLevel, propertyTester)
    }.getOrNull()
}
