package org.plan.research.minimization.plugin

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.core.algorithm.dd.impl.DDMin
import org.plan.research.minimization.core.algorithm.dd.impl.ProbabilisticDD
import org.plan.research.minimization.plugin.execution.DumbCompiler
import org.plan.research.minimization.plugin.execution.comparable.SimpleExceptionComparator
import org.plan.research.minimization.plugin.execution.gradle.GradleBuildExceptionProvider
import org.plan.research.minimization.plugin.execution.transformer.PathRelativizationTransformation
import org.plan.research.minimization.plugin.hierarchy.FileTreeHierarchyGenerator
import org.plan.research.minimization.plugin.lenses.FileDeletingItemLens
import org.plan.research.minimization.plugin.logging.withLog
import org.plan.research.minimization.plugin.model.BuildExceptionProvider
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.ProjectFileDDItem
import org.plan.research.minimization.plugin.model.ProjectHierarchyProducer
import org.plan.research.minimization.plugin.model.exception.CompilationException
import org.plan.research.minimization.plugin.model.exception.ExceptionTransformation
import org.plan.research.minimization.plugin.model.snapshot.SnapshotManager
import org.plan.research.minimization.plugin.model.state.*
import org.plan.research.minimization.plugin.snapshot.ProjectCloningSnapshotManager

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile

fun SnapshotStrategy.getSnapshotManager(project: Project): SnapshotManager =
    when (this) {
        SnapshotStrategy.PROJECT_CLONING -> ProjectCloningSnapshotManager(project)
    }

fun HierarchyCollectionStrategy.getHierarchyCollectionStrategy(): ProjectHierarchyProducer<*> =
    when (this) {
        HierarchyCollectionStrategy.FILE_TREE -> FileTreeHierarchyGenerator()
    }

fun DDStrategy.getDDAlgorithm(): DDAlgorithm =
    when (this) {
        DDStrategy.DD_MIN -> DDMin()
        DDStrategy.PROBABILISTIC_DD -> ProbabilisticDD()
    }.withLog()

fun CompilationStrategy.getCompilationStrategy(): BuildExceptionProvider =
    when (this) {
        CompilationStrategy.GRADLE_IDEA -> GradleBuildExceptionProvider()
        CompilationStrategy.DUMB -> DumbCompiler
    }

fun VirtualFile.getAllNestedElements(): List<VirtualFile> = buildList {
    VfsUtilCore.iterateChildrenRecursively(
        this@getAllNestedElements,
        null,
    ) {
        add(it)
        true
    }
}

fun List<VirtualFile>.getAllParents(root: VirtualFile): List<VirtualFile> = buildSet {
    @Suppress("AVOID_NESTED_FUNCTIONS")
    fun traverseParents(vertex: VirtualFile?) {
        if (vertex == null || contains(vertex) || VfsUtil.isAncestor(vertex, root, false)) {
            return
        }
        add(vertex)
        traverseParents(vertex.parent)
    }
    this@getAllParents.forEach(::traverseParents)
}.toList()

fun List<ProjectFileDDItem>.toVirtualFiles(context: IJDDContext): List<VirtualFile> =
    mapNotNull { it.getVirtualFile(context) }

fun ExceptionComparingStrategy.getExceptionComparator() = when (this) {
    ExceptionComparingStrategy.SIMPLE -> SimpleExceptionComparator()
}

fun TransformationDescriptors.getExceptionTransformations() = when (this) {
    TransformationDescriptors.PATH_RELATIVIZATION -> PathRelativizationTransformation()
}

fun ProjectItemLensDescriptor.getLens() = when (this) {
    ProjectItemLensDescriptor.FILE_DELETING -> FileDeletingItemLens()
}

suspend fun CompilationException.apply(transformations: List<ExceptionTransformation>, context: IJDDContext) =
    transformations.fold(this) { acc, it -> acc.apply(it, context) }
