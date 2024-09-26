package org.plan.research.minimization.plugin.hierarchy

import arrow.core.raise.option
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HDDLevel
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDDGenerator
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.plugin.model.PsiDDItem

abstract class AbstractIJHierarchicalDDGenerator(
    protected val propertyTester: PropertyTester<PsiDDItem>
) : HierarchicalDDGenerator<PsiDDItem> {

    override suspend fun generateNextLevel(minimizedLevel: List<PsiDDItem>): HDDLevel<PsiDDItem>? = option {
        val nextLevel = minimizedLevel.flatMap { it.psi.children.toList() }.map { PsiDDItem(it) }
        ensure(nextLevel.isNotEmpty())
        HDDLevel(nextLevel, propertyTester)
    }.getOrNull()
}