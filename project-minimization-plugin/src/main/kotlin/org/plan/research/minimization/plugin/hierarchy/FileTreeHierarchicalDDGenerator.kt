package org.plan.research.minimization.plugin.hierarchy

import arrow.core.raise.option
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HDDLevel
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDDGenerator
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.VirtualFileDDItem

class FileTreeHierarchicalDDGenerator(
    val project: Project,
    private val propertyTester: PropertyTester<IJDDContext, VirtualFileDDItem>
) : HierarchicalDDGenerator<IJDDContext, VirtualFileDDItem> {
    override suspend fun generateFirstLevel(): HDDLevel<IJDDContext, VirtualFileDDItem> {
        val projectRoot = project.guessProjectDir()
        val level = listOfNotNull(projectRoot).map { VirtualFileDDItem(it)}
        return HDDLevel(IJDDContext(project, level), level, propertyTester)
    }

    override suspend fun generateNextLevel(minimizationResult: DDAlgorithmResult<IJDDContext, VirtualFileDDItem>) =
        option {
            val nextFiles = minimizationResult.items.flatMap { it.vfs.children.asList() }.map { VirtualFileDDItem(it) }
            ensure(nextFiles.isNotEmpty())
            HDDLevel(minimizationResult.context.copy(currentLevel = nextFiles), nextFiles, propertyTester)
        }.getOrNull()
}
