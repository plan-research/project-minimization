package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.PsiChildrenPathDDItem
import org.plan.research.minimization.plugin.psi.PsiUtils

import com.intellij.openapi.application.readAction
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
import com.intellij.util.indexing.FileBasedIndex
import mu.KotlinLogging
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.kotlin.config.SourceKotlinRootType
import org.jetbrains.kotlin.config.TestSourceKotlinRootType
import org.jetbrains.kotlin.idea.KotlinFileType
import org.plan.research.minimization.plugin.model.PsiStubDDItem

import kotlin.io.path.pathString
import kotlin.io.path.relativeTo

/**
 * Service that provides functions to get a list of all the psi elements that could be modified
 */
@Service(Service.Level.APP)
class MinimizationPsiManagerService {
    private val logger = KotlinLogging.logger {}

    suspend fun findAllPsiWithBodyItems(context: IJDDContext): List<PsiChildrenPathDDItem> =
        findPsiInKotlinFiles(context, PsiChildrenPathDDItem.BODY_REPLACEABLE_PSI_JAVA_CLASSES)
            .filter { readAction { PsiChildrenPathDDItem.hasBodyIfAvailable(it) != false } }
            .mapNotNull { readAction { PsiUtils.buildReplaceablePsiItem(context, it) } }

    suspend fun findDeletablePsiItems(context: IJDDContext): List<PsiStubDDItem> =
        findPsiInKotlinFiles(context, PsiStubDDItem.DELETABLE_PSI_JAVA_CLASSES)
            .mapNotNull { readAction { PsiUtils.buildDeletablePsiItem(context, it) }.getOrNull() }

    suspend fun findAllKotlinFilesInIndexProject(context: IJDDContext): List<VirtualFile> =
        smartReadAction(context.indexProject) {
            val roots = service<RootsManagerService>().findPossibleRoots(context)
            logger.debug {
                "Found ${roots.size} roots: $roots"
            }
            val rootFiles = roots.mapNotNull {
                context.indexProjectDir.findFileByRelativePath(it.pathString)
            }
            val scope = GlobalSearchScopes.directoriesScope(context.indexProject, true, *rootFiles.toTypedArray())
                .intersectWith(SourcesScope(context.indexProject))
            FileTypeIndex.getFiles(KotlinFileType.INSTANCE, scope).toList()
        }

    private suspend fun <T : PsiElement> findPsiInKotlinFiles(
        context: IJDDContext,
        classes: List<Class<out T>>,
    ): List<T> {
        val kotlinFiles = findAllKotlinFilesInIndexProject(context)
        logFoundKotlinFiles(context, kotlinFiles)
        return extractAllPsi(context, kotlinFiles, classes)
    }

    private suspend fun <T : PsiElement> extractAllPsi(
        context: IJDDContext,
        files: Collection<VirtualFile>,
        classes: List<Class<out T>>,
    ): List<T> =
        files.flatMap { kotlinFile ->
            smartReadAction(context.indexProject) {
                val relativePath = kotlinFile.toNioPath().relativeTo(context.indexProjectDir.toNioPath())
                val fileInCurrentProject = context.projectDir.findFileByRelativePath(relativePath.pathString)
                    ?: return@smartReadAction emptyList()

                val ktFileInCurrentProject = PsiUtils.getKtFile(context, fileInCurrentProject)
                    ?: return@smartReadAction emptyList()

                classes.flatMap { clazz ->
                    PsiTreeUtil.collectElementsOfType(ktFileInCurrentProject, clazz)
                        .also {
                            logger.trace {
                                "Found ${it.size} ${clazz.simpleName} elements in $relativePath"
                            }
                        }
                }
            }
        }

    /**
     * A function that provides all available file types in the project with the files of that file type.
     * Used only in tracing logging.
     *
     * @param files a list of project source root to fetch files from
     * @return a map from file type to files of that type
     */
    private fun getAllFileTypesInProject(context: IJDDContext, files: List<VirtualFile>) = buildMap {
        FileBasedIndex.getInstance().processAllKeys(
            FileTypeIndex.NAME,
            { fileType ->
                val filesOfType = FileBasedIndex.getInstance().getContainingFiles(
                    FileTypeIndex.NAME,
                    fileType,
                    GlobalSearchScopes.directoriesScope(context.indexProject, true, *files.toTypedArray()),
                )
                if (filesOfType.isNotEmpty()) {
                    put(fileType, filesOfType.toList())
                }
                true
            },
            context.indexProject,
        )
    }

    private fun logFoundKotlinFiles(context: IJDDContext, files: List<VirtualFile>) {
        logger.debug { "Found ${files.size} kotlin files" }
        if (files.isEmpty()) {
            logger.warn { "Found 0 kotlin files!" }
            logger.trace {
                val fileTypes = getAllFileTypesInProject(context, files)
                val asString = fileTypes
                    .toList()
                    .map { (type, files) ->
                        "${type.name}: ${
                            files.map {
                                    it.toNioPath().relativeTo(context.projectDir.toNioPath())
                                }
                        }"
                    }
                "However, there are fileTypes and its files:\n$asString"
            }
        }
    }

    private class SourcesScope(project: Project) : GlobalSearchScope(project) {
        private val index = ProjectFileIndex.getInstance(project)

        override fun contains(file: VirtualFile): Boolean =
            index.isUnderSourceRootOfType(
                file, setOf(
                    SourceKotlinRootType, TestSourceKotlinRootType,
                    JavaSourceRootType.SOURCE, JavaSourceRootType.TEST_SOURCE,
                ),
            )

        override fun isSearchInModuleContent(aModule: Module): Boolean = true
        override fun isSearchInLibraries(): Boolean = false
    }
}
