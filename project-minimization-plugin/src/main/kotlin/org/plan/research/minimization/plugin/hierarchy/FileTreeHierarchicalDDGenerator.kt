package org.plan.research.minimization.plugin.hierarchy

import arrow.core.raise.option
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HDDLevel
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDDGenerator
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.plugin.model.dd.IJDDContext
import org.plan.research.minimization.plugin.model.dd.IJDDItem.VirtualFileDDItem
import org.plan.research.minimization.plugin.services.SnapshottingService
import org.plan.research.minimization.plugin.snapshot.VirtualFileProjectModifier

class FileTreeHierarchicalDDGenerator(
    val project: Project,
    private val propertyTester: PropertyTester<IJDDContext, VirtualFileDDItem>
) : HierarchicalDDGenerator<IJDDContext, VirtualFileDDItem> {
    private val snapshottingService = project.service<SnapshottingService>()
    private val projectModifier = project.service<VirtualFileProjectModifier>()
    override suspend fun generateFirstLevel(): HDDLevel<IJDDContext, VirtualFileDDItem> {
        val projectRoot = project.guessProjectDir()
        val level = listOfNotNull(projectRoot)
        val initialSnapshot = snapshottingService
            .initialSnapshot()
            .getOrNull()
            ?: error("Initial snapshot can't be initialized")

        return HDDLevel(IJDDContext(initialSnapshot), level.map { VirtualFileDDItem(it) }, propertyTester)
    }

    override suspend fun generateNextLevel(minimizationResult: DDAlgorithmResult<IJDDContext, VirtualFileDDItem>) =
        option {
            val nextFiles = minimizationResult.items.flatMap { it.vfs.children.asList() }.map { VirtualFileDDItem(it) }
            ensure(nextFiles.isNotEmpty())

            val modifyingFunction = projectModifier.modifyWith(minimizationResult.context, nextFiles)
            ensureNotNull(modifyingFunction)
            val newProjectSnapshot = snapshottingService.makeTransaction(minimizationResult.context.snapshot) {
                modifyingFunction(it)
                true
            }
            minimizationResult.context.snapshot.rollback().getOrNone().bind()

            HDDLevel(
                items = nextFiles,
                context = IJDDContext(newProjectSnapshot.getOrNone().bind()),
                propertyTester = propertyTester
            )
        }.getOrNull()

}
