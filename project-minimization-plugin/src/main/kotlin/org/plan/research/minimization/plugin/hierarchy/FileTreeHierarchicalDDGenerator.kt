package org.plan.research.minimization.plugin.hierarchy

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HDDLevel
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.VirtualFileDDItem
import org.plan.research.minimization.plugin.services.ProjectCloningService

class FileTreeHierarchicalDDGenerator(
    val project: Project,
    propertyTester: PropertyTester<IJDDContext, VirtualFileDDItem>
) : AbstractIJHierarchicalDDGenerator(propertyTester) {
    private val projectCloner = project.service<ProjectCloningService>()
    override suspend fun generateFirstLevel(): HDDLevel<IJDDContext, VirtualFileDDItem> {
        val projectRoot = project.guessProjectDir()
        val level = listOfNotNull(projectRoot)
        return HDDLevel(IJDDContext(project), level.map { VirtualFileDDItem(it) }, propertyTester)
    }

    override suspend fun generateNextLevel(minimizationResult: DDAlgorithmResult<IJDDContext, VirtualFileDDItem>): HDDLevel<IJDDContext, VirtualFileDDItem>? {
        val superResult = super.generateNextLevel(minimizationResult) ?: return null
        if (superResult.items.isEmpty()) return null
        val allCopiedNodes = getAllLevels().flatMap { it.items } + superResult.items
        val newProjectVersion =
            projectCloner.clone(minimizationResult.context.project, allCopiedNodes.map(VirtualFileDDItem::vfs))
                ?: return null
        return superResult
            .copy(items = superResult.items, context = IJDDContext(newProjectVersion))
            .takeIf { it.items.isNotEmpty() }
    }
}
