package org.plan.research.minimization.plugin.hierarchy

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HDDLevel
import org.plan.research.minimization.core.model.lift
import org.plan.research.minimization.plugin.model.IJHierarchicalDDGenerator
import org.plan.research.minimization.plugin.model.IJPropertyTester
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.item.ProjectFileDDItem
import org.plan.research.minimization.plugin.model.monad.SnapshotWithProgressMonad
import org.plan.research.minimization.plugin.services.RootsManagerService

import arrow.core.raise.option
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VirtualFile

import java.nio.file.Path

import kotlin.io.path.pathString
import kotlin.io.path.relativeTo

class FileTreeHierarchicalDDGenerator<C : IJDDContext>(
    private val propertyTester: IJPropertyTester<C, ProjectFileDDItem>,
) : IJHierarchicalDDGenerator<C, ProjectFileDDItem> {
    private lateinit var reporter: ProgressReporter

    context(SnapshotWithProgressMonad<C>)
    override suspend fun generateFirstLevel() =
        option {
            val level = lift {
                smartReadAction(context.indexProject) {
                    val rootManager = service<RootsManagerService>()
                    val roots = rootManager.findPossibleRoots(context)

                    reporter = ProgressReporter(context, roots)

                    roots.map { ProjectFileDDItem(it) }
                }
            }

            reporter.updateProgress(level)

            HDDLevel(level, propertyTester)
        }

    context(SnapshotWithProgressMonad<C>)
    override suspend fun generateNextLevel(minimizationResult: DDAlgorithmResult<ProjectFileDDItem>) =
        option {
            val nextFiles = lift {
                minimizationResult.retained.flatMap {
                    val vf = it.getVirtualFile(context) ?: return@flatMap emptyList()
                    readAction { vf.children }
                        .map { file ->
                            ProjectFileDDItem.create(context, file)
                        }
                }
            }
            ensure(nextFiles.isNotEmpty())

            reporter.updateProgress(nextFiles)

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
     * @constructor Creates a new instance of [ProgressReporter] based on the given root path and the array of root files.
     */
    context(SnapshotWithProgressMonad<C>)
    private inner class ProgressReporter(context: C, roots: List<Path>) {
        @Volatile
        private var currentLevel = 0
        private val levelMaxDepths = HashMap<Path, Int>()

        init {
            computeLevels(context, roots)
        }

        /**
         * Computes the maximum depth levels for the given root paths and their descendants.
         * This method implements DFS traversal via call-stack simulation.
         *
         * @param context The context of the project.
         * @param roots An array of VirtualFile representing the starting points to compute levels.
         */
        private fun computeLevels(context: C, roots: List<Path>) {
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
            nextStep(currentLevel, maxDepth)
        }
    }

    private data class StackEntry(val file: VirtualFile, val level: Int, var nextChildIndex: Int = 0)
}
