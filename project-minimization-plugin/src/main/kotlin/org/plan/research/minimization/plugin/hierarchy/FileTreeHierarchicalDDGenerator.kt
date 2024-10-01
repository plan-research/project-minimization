package org.plan.research.minimization.plugin.hierarchy

import arrow.core.raise.option
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HDDLevel
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDDGenerator
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.VirtualFileDDItem
import org.plan.research.minimization.plugin.services.ProjectCloningService

class FileTreeHierarchicalDDGenerator(
    val project: Project,
    private val propertyTester: PropertyTester<IJDDContext, VirtualFileDDItem>
) : HierarchicalDDGenerator<IJDDContext, VirtualFileDDItem> {
    private val projectCloner = project.service<ProjectCloningService>()
    private val savedLevels = mutableListOf<DDAlgorithmResult<IJDDContext, VirtualFileDDItem>>()
    override suspend fun generateFirstLevel(): HDDLevel<IJDDContext, VirtualFileDDItem> {
        val projectRoot = project.guessProjectDir()
        val level = listOfNotNull(projectRoot)
        return HDDLevel(IJDDContext(project), level.map { VirtualFileDDItem(it) }, propertyTester)
    }

    override suspend fun generateNextLevel(minimizationResult: DDAlgorithmResult<IJDDContext, VirtualFileDDItem>) =
        option {
            savedLevels.add(minimizationResult)
            val nextFiles = minimizationResult.items.flatMap { it.vfs.children.asList() }.map { VirtualFileDDItem(it) }
            ensure(nextFiles.isNotEmpty())
            val newProjectVersion =
                projectCloner.clone(minimizationResult.context.project, nextFiles.map(VirtualFileDDItem::vfs))
            ensureNotNull(newProjectVersion)
            HDDLevel(items = nextFiles, context = IJDDContext(newProjectVersion), propertyTester = propertyTester)
        }.getOrNull()

    fun getAllLevels(): List<DDAlgorithmResult<IJDDContext, VirtualFileDDItem>> = savedLevels
}
