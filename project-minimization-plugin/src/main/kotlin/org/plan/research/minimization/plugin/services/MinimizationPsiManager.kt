package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.PsiDDItem
import org.plan.research.minimization.plugin.psi.PsiProcessor

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.FileBasedIndex
import mu.KotlinLogging
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.*

import kotlin.io.path.relativeTo

/**
 * Service that provides functions to
 *  * Obtain a list of all the psi elements that could be modified
 *  * Modify the body of that object to replace it with `TODO()`
 *
 */
@Service(Service.Level.PROJECT)
class MinimizationPsiManager(private val rootProject: Project) {
    private val logger = KotlinLogging.logger {}
    private val psiProcessor = PsiProcessor(rootProject)

    suspend fun replaceBody(element: PsiElement): Unit = when (element) {
        is KtClassInitializer -> psiProcessor.replaceBody(element)
        is KtNamedFunction -> psiProcessor.replaceBody(element)
        is KtLambdaExpression -> psiProcessor.replaceBody(element)
        is KtPropertyAccessor -> psiProcessor.replaceBody(element)
        else -> error("Invalid PSI element type: ${element::class.simpleName}. Expected one of: KtClassInitializer, KtNamedFunction, KtLambdaExpression, KtPropertyAccessor")
    }

    suspend fun findAllPsiWithBodyItems(): List<PsiDDItem> =
        findPsiInKotlinFiles(PsiDDItem.BODY_REPLACEABLE_PSI_JAVA_CLASSES)
            .filter { PsiDDItem.hasBodyIfAvailable(it) != false }
            .mapNotNull { readAction { psiProcessor.buildReplaceablePsiItem(it) } }

    suspend fun findDeletablePsiItems(): List<PsiDDItem> =
        findPsiInKotlinFiles(PsiDDItem.DELETABLE_PSI_JAVA_CLASSES)
            .map { readAction { psiProcessor.buildDeletablePsiItem(it) } }

    private suspend fun <T : PsiElement> findPsiInKotlinFiles(classes: List<Class<out T>>): List<T> {
        val rootManager = service<RootsManagerService>()
        val roots = smartReadAction(rootProject) {
            rootManager
                .findPossibleRoots(IJDDContext(rootProject))
                .takeIf { it.isNotEmpty() }
                ?: listOf(rootProject.guessProjectDir()!!)
        }
        logger.debug {
            val root = rootProject.guessProjectDir()!!.toNioPath()
            "Found ${roots.size} roots: ${roots.map { it.toNioPath().relativeTo(root) }}"
        }
        val kotlinFiles = smartReadAction(rootProject) {
            FileTypeIndex.getFiles(
                KotlinFileType.INSTANCE,
                GlobalSearchScopes.directoriesScope(rootProject, true, *roots.toTypedArray()),
            ).toList()
        }
        logFoundKotlinFiles(kotlinFiles)
        return extractAllPsi(kotlinFiles, classes)
    }

    /**
     * Transforms a PsiDDItem to a corresponding PsiElement
     *
     * @param item
     */
    suspend fun getPsiElementFromItem(item: PsiDDItem): KtExpression? {
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

    private suspend fun <T : PsiElement> extractAllPsi(
        files: Collection<VirtualFile>,
        classes: List<Class<out T>>,
    ): List<T> =
        files.flatMap { kotlinFile ->
            val ktFile = readAction { psiProcessor.getKtFile(kotlinFile) } ?: return@flatMap emptyList()
            smartReadAction(rootProject) {
                classes.flatMap { clazz ->
                    PsiTreeUtil.collectElementsOfType(ktFile, clazz)
                        .also {
                            logger.debug {
                                val projectRoot = rootProject.guessProjectDir()!!.toNioPath()
                                "Found ${it.size} ${clazz.simpleName} elements in ${
                                    kotlinFile.toNioPath().relativeTo(projectRoot)
                                }"
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
    private fun getAllFileTypesInProject(files: List<VirtualFile>) = buildMap {
        FileBasedIndex.getInstance().processAllKeys(
            FileTypeIndex.NAME,
            { fileType ->
                val filesOfType = FileBasedIndex.getInstance().getContainingFiles(
                    FileTypeIndex.NAME,
                    fileType,
                    GlobalSearchScopes.directoriesScope(rootProject, true, *files.toTypedArray()),
                )
                if (filesOfType.isNotEmpty()) {
                    put(fileType, filesOfType.toList())
                }
                true
            },
            rootProject,
        )
    }

    private fun logFoundKotlinFiles(files: List<VirtualFile>) {
        logger.debug { "Found ${files.size} kotlin files" }
        if (files.isEmpty()) {
            logger.warn { "Found 0 kotlin files!" }
            logger.trace {
                val fileTypes = getAllFileTypesInProject(files)
                val asString = fileTypes
                    .toList()
                    .map { (type, files) ->
                        "${type.name}: ${
                            files.map {
                                    it.toNioPath().relativeTo(rootProject.guessProjectDir()!!.toNioPath())
                                }
                        }"
                    }
                "However, there are fileTypes and its files:\n$asString"
            }
        }
    }
}
