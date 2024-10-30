package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem
import org.plan.research.minimization.plugin.psi.PsiProcessor

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.psi.util.PsiTreeUtil
import mu.KotlinLogging
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.*

private typealias ClassKtExpression = Class<out KtExpression>

@Service(Service.Level.PROJECT)
class PsiAndRootManagerService(private val rootProject: Project) {
    private val psiProcessor = PsiProcessor(rootProject)
    private val logger = KotlinLogging.logger { }
    suspend fun findAllPsiWithBodyItems(): List<PsiWithBodyDDItem> {
        val roots = smartReadAction(rootProject) {
            findPossibleRoots().takeIf { it.isNotEmpty() } ?: listOf(rootProject.guessProjectDir()!!)
        }
        val kotlinFiles = smartReadAction(rootProject) {
            FileTypeIndex.getFiles(
                KotlinFileType.INSTANCE,
                GlobalSearchScopes.directoriesScope(rootProject, true, *roots.toTypedArray()),
            )
        }
        return extractAllPsi(kotlinFiles).mapNotNull { readAction { psiProcessor.getPsiElementParentPath(it) } }
    }

    suspend fun getPsiElementFromItem(item: PsiWithBodyDDItem): KtExpression? {
        val file = smartReadAction(rootProject) {
            rootProject.guessProjectDir()!!.findFileByRelativePath(item.localPath.toString())!!
        }
        val ktFile = readAction { psiProcessor.getKtFile(file)!! }
        val psiElement = readAction {
            var currentDepth = 0
            var element: PsiElement = ktFile
            while (currentDepth < item.childrenPath.size) {
                element = element.children[item.childrenPath[currentDepth++]]
            }
            element as? KtExpression
        }
        return psiElement
    }

    private suspend fun extractAllPsi(files: Collection<VirtualFile>): List<KtExpression> =
        files.flatMap { kotlinFile ->
            val ktFile = readAction { psiProcessor.getKtFile(kotlinFile) } ?: return@flatMap emptyList()

            val allPsiClasses: List<ClassKtExpression> = listOf(
                KtNamedFunction::class.java,
                KtPropertyAccessor::class.java,
                KtLambdaExpression::class.java,
                KtClassInitializer::class.java,
            )
            smartReadAction(rootProject) {
                allPsiClasses.flatMap { clazz ->
                    PsiTreeUtil.collectElementsOfType(ktFile, clazz)
                        .filter { it !is KtDeclarationWithBody || it.hasBody() }
                }
            }
        }

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

    fun findPossibleRoots(): List<VirtualFile> {
        val rootManager = ProjectRootManager.getInstance(rootProject)

        val sourceRoots = rootManager.contentSourceRoots.toList()
        val contentRoots = rootManager.contentRoots.toList()
        val srcRoots = contentRoots.mapNotNull { it.findChild("src") }

        return propagateAndMergeRoots(contentRoots, srcRoots, sourceRoots)
    }

    private enum class PropagationStatus {
        ALREADY_PROPAGATED_OR_ADDED,
        IS_CONTENT_ROOT,
        NEED_TO_PROPAGATE,
        ;
    }
}
