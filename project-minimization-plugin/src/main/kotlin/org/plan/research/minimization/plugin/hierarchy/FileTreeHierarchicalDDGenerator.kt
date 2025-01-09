package org.plan.research.minimization.plugin.hierarchy

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HDDLevel
import org.plan.research.minimization.plugin.model.IJHierarchicalDDGenerator
import org.plan.research.minimization.plugin.model.IJPropertyTester
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.context.IJDDContextMonad
import org.plan.research.minimization.plugin.model.item.ProjectFileDDItem
import org.plan.research.minimization.plugin.services.RootsManagerService

import arrow.core.raise.option
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.util.progress.SequentialProgressReporter

import java.nio.file.Path

import kotlin.io.path.pathString
import kotlin.io.path.relativeTo

class FileTreeHierarchicalDDGenerator<C : IJDDContext>(
    private val propertyTester: IJPropertyTester<C, ProjectFileDDItem>,
) : IJHierarchicalDDGenerator<C, ProjectFileDDItem> {
    private var reporter: ProgressReporter? = null

    context(IJDDContextMonad<C>)
    override suspend fun generateFirstLevel() =
        option {
            val level = smartReadAction(context.indexProject) {
                val rootManager = service<RootsManagerService>()
                val roots = rootManager.findPossibleRoots(context)

                context.progressReporter?.let {
                    reporter = ProgressReporter(it, context, roots)
                }

                roots.map { ProjectFileDDItem(it) }
            }

            reporter?.updateProgress(level)

            updateContext {
                @Suppress("UNCHECKED_CAST")
                it.copy(currentLevel = level) as C
            }

            HDDLevel(level, propertyTester)
        }

    context(IJDDContextMonad<C>)
    override suspend fun generateNextLevel(minimizationResult: DDAlgorithmResult<ProjectFileDDItem>) =
        option {
            val nextFiles = minimizationResult.flatMap {
                val vf = it.getVirtualFile(context) ?: return@flatMap emptyList()
                readAction { vf.children }
                    .map { file ->
                        ProjectFileDDItem.create(context, file)
                    }
            }
            ensure(nextFiles.isNotEmpty())

            reporter?.updateProgress(nextFiles)

            updateContext {
                @Suppress("UNCHECKED_CAST")
                it.copy(currentLevel = nextFiles) as C
            }

            HDDLevel(nextFiles, propertyTester)
        }

    /**
     * ProgressReporter is a class responsible for managing and reporting the progress of a hierarchical
     * delta-debugging process on a file tree structure. It utilizes a sequential progress reporter to
     * keep track of the current progress and update it during the process. This class computes levels
     * of directories and files to provide meaningful progress reporting.
     *
     * @param context The context of the project.
     * @param roots An array of VirtualFile representing the starting points to compute levels.
     * @property reporter An instance of [SequentialProgressReporter] used for updating the progress.
     * @constructor Creates a new instance of [ProgressReporter] based on the given root path and the array of root files.
     */
    private class ProgressReporter(val reporter: SequentialProgressReporter, context: IJDDContext, roots: List<Path>) {
        @Volatile
        private var currentLevel = 0
        private val levelMaxDepths = HashMap<Path, Int>()

        init {
            reporter.nextStep(1)
            computeLevels(context, roots)
        }

        /**
         * Computes the maximum depth levels for the given root paths and their descendants.
         * This method implements DFS traversal via call-stack simulation.
         *
         * @param context The context of the project.
         * @param roots An array of VirtualFile representing the starting points to compute levels.
         */
        private fun computeLevels(context: IJDDContext, roots: List<Path>) {
            val stack = mutableListOf<StackEntry>()
            roots.forEach { root ->
                context.projectDir.findFileByRelativePath(root.pathString)?.let {
                    stack.add(StackEntry(it, 1))
                }
            }

            val root = context.projectDir.toNioPath()
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
