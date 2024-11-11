package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem
import org.plan.research.minimization.plugin.psi.PsiUtils

import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.psi.util.PsiTreeUtil
import mu.KotlinLogging
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.kotlin.config.SourceKotlinRootType
import org.jetbrains.kotlin.config.TestSourceKotlinRootType
import org.jetbrains.kotlin.idea.KotlinFileType

import kotlin.io.path.pathString
import kotlin.io.path.relativeTo

/**
 * Service that provides functions to get a list of all the psi elements that could be modified
 */
@Service(Service.Level.APP)
class MinimizationPsiManagerService {
    private val logger = KotlinLogging.logger {}

    suspend fun findAllKotlinFilesInIndexProject(context: IJDDContext): List<VirtualFile> =
        smartReadAction(context.indexProject) {
            val roots = service<RootsManagerService>().findPossibleRoots(context).mapNotNull {
                context.indexProjectDir.findFileByRelativePath(it.pathString)
            }
            val scope = GlobalSearchScopes.directoriesScope(context.indexProject, true, *roots.toTypedArray())
                .intersectWith(SourcesScope(context.indexProject))
            FileTypeIndex.getFiles(KotlinFileType.INSTANCE, scope).toList()
        }

    suspend fun findAllPsiWithBodyItems(context: IJDDContext): List<PsiWithBodyDDItem> {
        val kotlinFiles = findAllKotlinFilesInIndexProject(context)
        logger.debug {
            "Found ${kotlinFiles.size} kotlin files"
        }
        if (kotlinFiles.isEmpty()) {
            logger.warn { "Found 0 kotlin files!" }
        }
        return extractAllPsiFromIndexProject(context, kotlinFiles)
    }

    private suspend fun extractAllPsiFromIndexProject(
        context: IJDDContext,
        files: Collection<VirtualFile>,
    ): List<PsiWithBodyDDItem> =
        files.flatMap { kotlinFile ->
            smartReadAction(context.indexProject) {
                val relativePath = kotlinFile.toNioPath().relativeTo(context.indexProjectDir.toNioPath())
                val fileInCurrentProject = context.projectDir.findFileByRelativePath(relativePath.pathString)
                    ?: return@smartReadAction emptyList()

                val ktFileInCurrentProject = PsiUtils.getKtFile(context, fileInCurrentProject)
                    ?: return@smartReadAction emptyList()

                PsiWithBodyDDItem.PSI_ALL_JAVA_CLASSES.flatMap { clazz ->
                    PsiTreeUtil.collectElementsOfType(ktFileInCurrentProject, clazz)
                        .filter { PsiWithBodyDDItem.hasBodyIfAvailable(it) != false }
                        .also {
                            logger.debug {
                                "Found ${it.size} ${clazz.simpleName} elements in $relativePath"
                            }
                        }.mapNotNull { PsiUtils.createPsiDDItem(context, it) }
                }
            }
        }

    private class SourcesScope(project: Project) : GlobalSearchScope(project) {
        private val index = ProjectFileIndex.getInstance(project)

        override fun contains(file: VirtualFile): Boolean =
            index.isUnderSourceRootOfType(file, setOf(
                SourceKotlinRootType, TestSourceKotlinRootType,
                JavaSourceRootType.SOURCE, JavaSourceRootType.TEST_SOURCE,
            ))

        override fun isSearchInModuleContent(aModule: Module): Boolean = true
        override fun isSearchInLibraries(): Boolean = false
    }
}
