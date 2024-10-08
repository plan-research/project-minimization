package org.plan.research.minimization.plugin.hierarchy

import arrow.core.raise.option
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HDDLevel
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDDGenerator
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.ProjectFileDDItem
import kotlin.io.path.Path

class FileTreeHierarchicalDDGenerator(
    private val propertyTester: PropertyTester<IJDDContext, ProjectFileDDItem>
) : HierarchicalDDGenerator<IJDDContext, ProjectFileDDItem> {
    override suspend fun generateFirstLevel(context: IJDDContext) =
        option {
            // TODO: take first level as root's children
            val level = listOf(ProjectFileDDItem(Path("")))
            HDDLevel(context.copy(currentLevel = level), level, propertyTester)
        }

    override suspend fun generateNextLevel(minimizationResult: DDAlgorithmResult<IJDDContext, ProjectFileDDItem>) =
        option {
            val nextFiles = minimizationResult.items.flatMap {
                val vf = it.getVirtualFile(minimizationResult.context) ?: return@flatMap emptyList()
                vf.children.map { file ->
                    ProjectFileDDItem.create(minimizationResult.context, file)
                }
            }
            ensure(nextFiles.isNotEmpty())
            HDDLevel(minimizationResult.context.copy(currentLevel = nextFiles), nextFiles, propertyTester)
        }
}
