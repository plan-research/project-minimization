package org.plan.research.minimization.plugin.hierarchy

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HDDLevel
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDDGenerator
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.ProjectFileDDItem

import arrow.core.raise.option
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.util.progress.SequentialProgressReporter

import java.nio.file.Path

import kotlin.io.path.relativeTo

class FileTreeHierarchicalDDGenerator(
    private val propertyTester: PropertyTester<IJDDContext, ProjectFileDDItem>,
) : HierarchicalDDGenerator<IJDDContext, ProjectFileDDItem> {
    private var reporter: ProgressReporter? = null

    private fun propagateAndMergeRoots(
        contentRoots: List<VirtualFile>,
        srcRoots: List<VirtualFile>,
        sourceRoots: List<VirtualFile>,
    ): List<VirtualFile> {
        // propagate src roots (replace them with their children) if it contains any content root
        val propagationStatus = HashMap<VirtualFile, PropagationStatus>()
        for (contentRoot in contentRoots) {
            propagationStatus[contentRoot] = PropagationStatus.IS_CONTENT_ROOT
            var parent: VirtualFile? = contentRoot.parent
            while (parent != null) {
                propagationStatus.putIfAbsent(parent, PropagationStatus.NEED_TO_PROPAGATE) ?: break
                parent = parent.parent
            }
        }

        val queue = ArrayDeque<VirtualFile>()
        queue.addAll(srcRoots)

        val roots = mutableListOf<VirtualFile>()
        while (queue.isNotEmpty()) {
            val root = queue.removeFirst()
            val status = propagationStatus[root]
            propagationStatus[root] = PropagationStatus.ALREADY_PROPAGATED_OR_ADDED
            when (status) {
                PropagationStatus.NEED_TO_PROPAGATE -> queue.addAll(root.children)
                PropagationStatus.IS_CONTENT_ROOT, PropagationStatus.ALREADY_PROPAGATED_OR_ADDED -> {}
                else -> roots.add(root)
            }
        }

        // delete all sourceRoots that are in any of the already added roots
        val sourceRootsToAdd = sourceRoots.filter { sourceRoot ->
            roots.none { src -> VfsUtil.isAncestor(src, sourceRoot, false) }
        }
        roots.addAll(sourceRootsToAdd)

        return roots
    }

    private fun findPossibleRoots(context: IJDDContext): List<VirtualFile> {
        val rootManager = ProjectRootManager.getInstance(context.project)

        val sourceRoots = rootManager.contentSourceRoots.toList()
        val contentRoots = rootManager.contentRoots.toList()
        val srcRoots = contentRoots.mapNotNull { it.findChild("src") }

        return propagateAndMergeRoots(contentRoots, srcRoots, sourceRoots)
    }

    override suspend fun generateFirstLevel(context: IJDDContext) =
        option {
            val level = smartReadAction(context.project) {
                val roots = findPossibleRoots(context).takeIf { it.isNotEmpty() } ?: listOf(context.projectDir)

                context.progressReporter?.let {
                    reporter = ProgressReporter(it, context.projectDir.toNioPath(), roots)
                }

                roots.map { ProjectFileDDItem.create(context, it) }
            }

            reporter?.updateProgress(level)
            HDDLevel(context.copy(currentLevel = level), level, propertyTester)
        }

    override suspend fun generateNextLevel(minimizationResult: DDAlgorithmResult<IJDDContext, ProjectFileDDItem>) =
        option {
            val nextFiles = minimizationResult.items.flatMap {
                val vf = it.getVirtualFile(minimizationResult.context) ?: return@flatMap emptyList()
                readAction { vf.children }
                    .map { file ->
                        ProjectFileDDItem.create(minimizationResult.context, file)
                    }
            }
            ensure(nextFiles.isNotEmpty())

            reporter?.updateProgress(nextFiles)
            HDDLevel(minimizationResult.context.copy(currentLevel = nextFiles), nextFiles, propertyTester)
        }

    private enum class PropagationStatus {
        ALREADY_PROPAGATED_OR_ADDED,
        IS_CONTENT_ROOT,
        NEED_TO_PROPAGATE,
        ;
    }

    /**
     * ProgressReporter is a class responsible for managing and reporting the progress of a hierarchical
     * delta-debugging process on a file tree structure. It utilizes a sequential progress reporter to
     * keep track of the current progress and update it during the process. This class computes levels
     * of directories and files to provide meaningful progress reporting.
     *
     * @param root The root path from which relative paths are computed and stored.
     * @param roots An array of VirtualFile representing the starting points to compute levels.
     * @property reporter An instance of [SequentialProgressReporter] used for updating the progress.
     * @constructor Creates a new instance of [ProgressReporter] based on the given root path and the array of root files.
     */
    private class ProgressReporter(val reporter: SequentialProgressReporter, root: Path, roots: List<VirtualFile>) {
        @Volatile
        private var currentLevel = 0
        private val levelMaxDepths = HashMap<Path, Int>()

        init {
            reporter.nextStep(1)
            computeLevels(root, roots)
        }

        /**
         * Computes the maximum depth levels for the given root paths and their descendants.
         * This method implements DFS traversal via call-stack simulation.
         *
         * @param root The root path from which relative paths are computed and stored.
         * @param roots An array of VirtualFile representing the starting points to compute levels.
         */
        private fun computeLevels(root: Path, roots: List<VirtualFile>) {
            val stack = mutableListOf<StackEntry>()
            roots.forEach {
                stack.add(StackEntry(it, 1))
            }

            while (stack.isNotEmpty()) {
                val entry = stack.last()

                val children = entry.file.children
                if (children.isNotEmpty()) {
                    if (entry.nextChildIndex < children.size) {
                        stack.add(StackEntry(children[entry.nextChildIndex], entry.level + 1))
                        entry.nextChildIndex += 1
                    } else {
                        stack.removeLast()
                        levelMaxDepths[entry.file.toNioPath().relativeTo(root)] =
                            children.maxOf { levelMaxDepths[it.toNioPath().relativeTo(root)]!! }
                    }
                } else {
                    stack.removeLast()
                    levelMaxDepths[entry.file.toNioPath().relativeTo(root)] = entry.level
                }
            }
        }

        fun updateProgress(level: List<ProjectFileDDItem>) {
            currentLevel += 1
            val maxDepth = level.maxOf { levelMaxDepths[it.localPath]!! }
            reporter.nextStep((100 * currentLevel) / maxDepth)
        }

        private data class StackEntry(val file: VirtualFile, val level: Int, var nextChildIndex: Int = 0)
    }
}
