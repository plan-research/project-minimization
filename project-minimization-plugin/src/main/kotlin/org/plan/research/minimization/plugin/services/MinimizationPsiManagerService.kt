package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.PsiChildrenIndexDDItem
import org.plan.research.minimization.plugin.model.PsiStubDDItem
import org.plan.research.minimization.plugin.psi.KotlinOverriddenElementsGetter
import org.plan.research.minimization.plugin.psi.PsiDSU
import org.plan.research.minimization.plugin.psi.PsiUtils

import arrow.core.compareTo
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.concurrency.annotations.RequiresReadLock
import mu.KotlinLogging
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.kotlin.config.SourceKotlinRootType
import org.jetbrains.kotlin.config.TestSourceKotlinRootType
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtElement

import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtValVarKeywordOwner
import kotlin.collections.singleOrNull
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo

private typealias CoreDsuElement = Pair<KtElement, PsiStubDDItem>

/**
 * Service that provides functions to get a list of all the psi elements that could be modified
 */
@Service(Service.Level.APP)
class MinimizationPsiManagerService {
    private val logger = KotlinLogging.logger {}

    suspend fun findAllPsiWithBodyItems(context: IJDDContext): List<PsiChildrenIndexDDItem> =
        smartReadAction(context.indexProject) {
            findPsiInKotlinFiles(context, PsiChildrenIndexDDItem.BODY_REPLACEABLE_PSI_JAVA_CLASSES)
                .filter { PsiChildrenIndexDDItem.hasBodyIfAvailable(it) != false }
                .mapNotNull { PsiUtils.buildReplaceablePsiItem(context, it) }
        }

    /**
     * Finds all deletable PSI items in Kotlin files within the given context.
     * The deletable elements are the elements that
     *  * are one of the [PsiStubDDItem.DELETABLE_PSI_JAVA_CLASSES] classes
     *  * on the path from [org.jetbrains.kotlin.psi.KtFile] to that element there are only serializable elements.
     *  That means that they could be represented via [org.plan.research.minimization.plugin.psi.stub.KtStub]
     *
     * @param context The context for the minimization process containing the current project and relevant properties.
     * @return A list of deletable PSI items found in the Kotlin files of the project.
     */
    suspend fun findDeletablePsiItems(context: IJDDContext): List<PsiStubDDItem> =
        smartReadAction(context.indexProject) {
            val nonStructuredItems = findPsiInKotlinFiles(context, PsiStubDDItem.DELETABLE_PSI_JAVA_CLASSES)
                .mapNotNull { psiElement ->
                    PsiUtils.buildDeletablePsiItem(context, psiElement).getOrNull()?.let { psiElement to it }
                } + getPrimaryConstructorProperties(context)

            val dsu = PsiDSU<KtElement, PsiStubDDItem>(nonStructuredItems) { lhs, rhs ->
                lhs.childrenPath.size
                    .compareTo(rhs.childrenPath.size)
                    .takeIf { it != 0 }
                    ?: lhs.childrenPath.compareTo(rhs.childrenPath)
            }
            dsu.transformItems(nonStructuredItems)
        }

    @RequiresReadLock
    fun findAllKotlinFilesInIndexProject(context: IJDDContext): List<VirtualFile> {
        val roots = service<RootsManagerService>().findPossibleRoots(context)
        logger.debug {
            "Found ${roots.size} roots: $roots"
        }
        val rootFiles = roots.mapNotNull {
            context.indexProjectDir.findFileByRelativePath(it.pathString)
        }
        val scope = GlobalSearchScopes.directoriesScope(context.indexProject, true, *rootFiles.toTypedArray())
            .intersectWith(SourcesScope(context.indexProject))
        return FileTypeIndex.getFiles(KotlinFileType.INSTANCE, scope).toList()
    }

    @RequiresReadLock
    private fun <T : PsiElement> findPsiInKotlinFiles(
        context: IJDDContext,
        classes: List<Class<out T>>,
    ): List<T> {
        val kotlinFiles = findAllKotlinFilesInIndexProject(context)
        logger.debug {
            "Found ${kotlinFiles.size} kotlin files"
        }
        if (kotlinFiles.isEmpty()) {
            logger.warn { "Found 0 kotlin files!" }
        }
        return extractAllPsi(context, kotlinFiles, classes)
    }

    @RequiresReadLock
    private fun <T : PsiElement> extractAllPsi(
        context: IJDDContext,
        files: Collection<VirtualFile>,
        classes: List<Class<out T>>,
    ): List<T> =
        files.flatMap { kotlinFile ->
            val relativePath = kotlinFile.toNioPath().relativeTo(context.indexProjectDir.toNioPath())
            val fileInCurrentProject = context.projectDir.findFileByRelativePath(relativePath.pathString)
                ?: return@flatMap emptyList()

            val ktFileInCurrentProject = PsiUtils.getKtFile(context, fileInCurrentProject)
                ?: return@flatMap emptyList()

            classes.flatMap { clazz ->
                PsiTreeUtil.collectElementsOfType(ktFileInCurrentProject, clazz)
                    .also {
                        logger.trace {
                            "Found ${it.size} ${clazz.simpleName} elements in $relativePath"
                        }
                    }
            }
        }

    private fun PsiDSU<KtElement, PsiStubDDItem>.transformItems(
        items: List<CoreDsuElement>,
    ): List<PsiStubDDItem> {
        items.forEach { (element) ->
            KotlinOverriddenElementsGetter
                .getOverriddenElements(element)
                .forEach { union(element, it) }
        }
        val classes = classes
        return items
            .asSequence()
            .filter { (psiElement, item) -> representativeElementOf(psiElement) === item }
            .map { (_, item) ->
                val clazz = classes[item]!!
                clazz.singleOrNull() ?: PsiStubDDItem.OverriddenPsiStubDDItem(
                    item.localPath,
                    item.childrenPath,
                    clazz.filter { it != item },
                )
            }
            .toList()
    }

    private fun getPrimaryConstructorProperties(context: IJDDContext): List<CoreDsuElement> {
        val primaryConstructors = findPsiInKotlinFiles(context, listOf(KtPrimaryConstructor::class.java))
        return primaryConstructors
            .asSequence()
            .flatMap { it.valueParameters }
            .filterIsInstance<KtValVarKeywordOwner>()
            .filterIsInstance<KtElement>()
            .mapNotNull { psiElement ->
                PsiUtils.buildDeletablePsiItem(context, psiElement).getOrNull()?.let { psiElement to it }
            }
            .toList()
    }

    private class SourcesScope(project: Project) : GlobalSearchScope(project) {
        private val index = ProjectFileIndex.getInstance(project)

        override fun contains(file: VirtualFile): Boolean =
            index.isUnderSourceRootOfType(
                file,
                setOf(
                    SourceKotlinRootType, TestSourceKotlinRootType,
                    JavaSourceRootType.SOURCE, JavaSourceRootType.TEST_SOURCE,
                ),
            )

        override fun isSearchInModuleContent(aModule: Module): Boolean = true
        override fun isSearchInLibraries(): Boolean = false
    }
}
