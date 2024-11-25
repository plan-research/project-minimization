package org.plan.research.minimization.plugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import mu.KotlinLogging
import org.plan.research.minimization.plugin.model.IJDDContext
import java.nio.file.Path
import kotlin.io.path.relativeTo

@Service(Service.Level.APP)
class RootsManagerService {
    private val logger = KotlinLogging.logger { }

    private fun propagateAndMergeRoots(
        contentRoots: List<VirtualFile>,
        srcRoots: List<VirtualFile>,
        sourceRoots: List<VirtualFile>,
        ignoreRoots: List<VirtualFile>
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

        // Add NEED_TO_PROPAGATE status to all parents of IgnoreRoots
        // Add IS_IGNORED for IgnoreRoots
        for (ignoreRoot in ignoreRoots) {
            propagationStatus[ignoreRoot] = PropagationStatus.IS_IGNORED
            var parent: VirtualFile? = ignoreRoot.parent
            while (parent != null) {
                propagationStatus.putIfAbsent(parent, PropagationStatus.NEED_TO_PROPAGATE) ?: break
                parent = parent.parent
            }
        }

        // Check if src is a child of any Ignore Root, don't add it in that case
        val queue = ArrayDeque<VirtualFile>()
        queue.addAll(srcRoots.filter { srcRoot ->
            ignoreRoots.none { VfsUtil.isAncestor(it, srcRoot, false) }
        })

        val roots = mutableListOf<VirtualFile>()
        while (queue.isNotEmpty()) {
            val root = queue.removeFirst()
            val status = propagationStatus[root]
            propagationStatus[root] = PropagationStatus.ALREADY_PROPAGATED_OR_ADDED
            when (status) {
                PropagationStatus.NEED_TO_PROPAGATE -> queue.addAll(root.children)
                PropagationStatus.IS_CONTENT_ROOT, PropagationStatus.IS_IGNORED, PropagationStatus.ALREADY_PROPAGATED_OR_ADDED -> {}
                else -> roots.add(root)
            }
        }

        // delete all sourceRoots that are in any of the already added roots or in any of Ignore Roots
        val sourceRootsToAdd = sourceRoots.filter { sourceRoot ->
            roots.none { src -> VfsUtil.isAncestor(src, sourceRoot, false) } &&
            ignoreRoots.none { VfsUtil.isAncestor(it, sourceRoot, false) }
        }
        roots.addAll(sourceRootsToAdd)

        return roots
    }

    fun findPossibleRoots(context: IJDDContext): List<Path> {
        val rootManager = ProjectRootManager.getInstance(context.indexProject)

        val sourceRoots = rootManager.contentSourceRoots.toList()
        val contentRoots = rootManager.contentRoots.toList()
        val srcRoots = contentRoots.mapNotNull { it.findChild("src") }

        // get list of Virtual Files using full string paths at ignorePaths
        val ignorePaths: List<String> = context.originalProject.service<MinimizationPluginSettings>().state.ignorePaths
        val ignoreRoots: List<VirtualFile> = ignorePaths.mapNotNull { relativePath ->
            VfsUtil.findRelativeFile(context.indexProjectDir, *relativePath.split("/").toTypedArray())
        }

        val mergedRoots = propagateAndMergeRoots(contentRoots, srcRoots, sourceRoots, ignoreRoots).takeIf { it.isNotEmpty() }
            ?: listOf(context.indexProjectDir)

        val indexRoot = context.indexProjectDir.toNioPath()
        val resultRoots = mergedRoots.map { it.toNioPath().relativeTo(indexRoot) }

        logger.debug {
            "Found ${resultRoots.size} roots: $resultRoots"
        }

        return resultRoots
    }

    private enum class PropagationStatus {
        ALREADY_PROPAGATED_OR_ADDED,
        IS_CONTENT_ROOT,
        IS_IGNORED,
        NEED_TO_PROPAGATE,
        ;
    }
}
