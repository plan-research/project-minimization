package org.plan.research.minimization.plugin.hierarchy

import arrow.core.raise.option
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HDDLevel
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDDGenerator
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.plugin.model.ProjectDDVersion
import org.plan.research.minimization.plugin.model.PsiDDItem

abstract class AbstractIJHierarchicalDDGenerator(
    protected val propertyTester: PropertyTester<ProjectDDVersion, PsiDDItem>
) : HierarchicalDDGenerator<ProjectDDVersion, PsiDDItem> {

    override suspend fun generateNextLevel(
        minimizationResult: DDAlgorithmResult<ProjectDDVersion, PsiDDItem>
    ): HDDLevel<ProjectDDVersion, PsiDDItem>? = option {
        val nextLevel = minimizationResult.items.flatMap { it.psi.children.toList() }.map { PsiDDItem(it) }
        ensure(nextLevel.isNotEmpty())
        HDDLevel(minimizationResult.version, nextLevel, propertyTester)
    }.getOrNull()
}
