package org.plan.research.minimization.plugin.hierarchy

import arrow.core.raise.option
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HDDLevel
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDDGenerator
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.VirtualFileDDItem

abstract class AbstractIJHierarchicalDDGenerator(
    protected val propertyTester: PropertyTester<IJDDContext, VirtualFileDDItem>
) : HierarchicalDDGenerator<IJDDContext, VirtualFileDDItem> {

    private val levelList = mutableListOf<DDAlgorithmResult<IJDDContext, VirtualFileDDItem>>()

    override suspend fun generateNextLevel(
        minimizationResult: DDAlgorithmResult<IJDDContext, VirtualFileDDItem>
    ): HDDLevel<IJDDContext, VirtualFileDDItem>? = option {
        levelList.add(minimizationResult)
        val nextLevel = minimizationResult.items.flatMap { it.vfs.children.toList() }.map { VirtualFileDDItem(it) }
        ensure(nextLevel.isNotEmpty())
        HDDLevel(minimizationResult.context, nextLevel, propertyTester)
    }.getOrNull()

    fun getAllLevels(): List<DDAlgorithmResult<IJDDContext, VirtualFileDDItem>> = levelList
}
