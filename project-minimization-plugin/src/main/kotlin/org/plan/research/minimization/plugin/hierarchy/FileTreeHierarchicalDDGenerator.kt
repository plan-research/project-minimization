package org.plan.research.minimization.plugin.hierarchy

import arrow.core.raise.option
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.util.progress.SequentialProgressReporter
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HDDLevel
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDDGenerator
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.ProjectFileDDItem
import java.nio.file.Path
import kotlin.io.path.relativeTo

class FileTreeHierarchicalDDGenerator(
    private val propertyTester: PropertyTester<IJDDContext, ProjectFileDDItem>
) : HierarchicalDDGenerator<IJDDContext, ProjectFileDDItem> {

    private var reporter: ProgressReporter? = null

    override suspend fun generateFirstLevel(context: IJDDContext) =
        option {
            val level = smartReadAction(context.project) {
                val roots = ProjectRootManager.getInstance(context.project).contentSourceRoots
                    .takeIf { it.isNotEmpty() } ?: arrayOf(context.projectDir)

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

    /**
     * ProgressReporter is a class responsible for managing and reporting the progress of a hierarchical
     * delta-debugging process on a file tree structure. It utilizes a sequential progress reporter to
     * keep track of the current progress and update it during the process. This class computes levels
     * of directories and files to provide meaningful progress reporting.
     *
     * @property reporter An instance of [SequentialProgressReporter] used for updating the progress.
     * @property currentLevel A volatile counter indicating the current level of progress.
     * @property levelMaxDepths A hash map that stores the maximum depth levels of paths relative to the root.
     * @constructor Creates a new instance of [ProgressReporter] based on the given root path and the array of root files.
     */
    private class ProgressReporter(val reporter: SequentialProgressReporter, root: Path, roots: Array<VirtualFile>) {
        @Volatile
        private var currentLevel = 0
        private val levelMaxDepths = HashMap<Path, Int>()

        init {
            computeLevels(root, roots)
        }

        private data class StackEntry(val file: VirtualFile, val level: Int, var nextChildIndex: Int = 0)

        /**
         * Computes the maximum depth levels for the given root paths and their descendants.
         * This method implements DFS traversal via call-stack simulation.
         *
         * @param root The root path from which relative paths are computed and stored.
         * @param roots An array of VirtualFile representing the starting points to compute levels.
         */
        private fun computeLevels(root: Path, roots: Array<VirtualFile>) {
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
            reporter.nextStep(100 * currentLevel / maxDepth)
        }
    }
}
