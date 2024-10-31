package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.command.writeCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileTypes.FileType
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
import org.jetbrains.kotlin.idea.base.util.projectScope
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtPsiFactory

import kotlin.io.path.relativeTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.base.util.projectScope
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.psi.PsiProcessor

/**
 * Service that provides functions to
 *  * Obtain a list of all the psi elements that could be modified
 *  * Modify the body of that object to replace it with `TODO()`
 *
 */
@Service(Service.Level.PROJECT)
class MinimizationPsiManager(private val rootProject: Project) {
    private val logger = KotlinLogging.logger {}
    private val psiFactory = KtPsiFactory(rootProject)
    private val psiProcessor = PsiProcessor(rootProject)

    suspend fun replaceBody(classInitializer: KtClassInitializer) {
        withContext(Dispatchers.EDT) {
            writeCommandAction(rootProject, "Replacing Class Initializer") {
                logger.debug { "Replacing class initializer body: ${classInitializer.name}" }
                classInitializer.body?.replace(psiFactory.createBlock(BLOCKLESS_TEXT))
            }
        }
    }

    suspend fun replaceBody(function: KtNamedFunction) {
        val (hasBlockBody, hasBody) = readAction { function.hasBlockBody() to function.hasBody() }
        when {
            hasBlockBody -> withContext(Dispatchers.EDT) {
                writeCommandAction(rootProject, "Replacing Function Body Block") {
                    logger.debug { "Replacing function body block: ${function.name} in ${function.containingFile.virtualFile.path}" }
                    function.bodyBlockExpression?.replace(
                        psiFactory.createBlock(
                            BLOCKLESS_TEXT,
                        ),
                    )
                }
            }

            hasBody -> withContext(Dispatchers.EDT) {
                writeCommandAction(rootProject, "Replacing Function Body Expression") {
                    logger.debug { "Replacing function body without block: ${function.name} in ${function.containingFile.virtualFile.path}" }
                    function.bodyExpression!!.replace(
                        psiFactory.createExpression(
                            BLOCKLESS_TEXT,
                        ),
                    )
                }
            }

            else -> {}
        }
    }

    suspend fun replaceBody(lambdaExpression: KtLambdaExpression): Unit = withContext(Dispatchers.EDT) {
        writeCommandAction(rootProject, "Replacing Lambda Body Expression") {
            logger.debug { "Replacing lambda expression in ${lambdaExpression.containingFile.virtualFile.path}" }
            lambdaExpression.bodyExpression!!.replace(
                psiFactory.createLambdaExpression(
                    "",
                    BLOCKLESS_TEXT,
                ).bodyExpression!!,
            )
        }
    }

    suspend fun replaceBody(accessor: KtPropertyAccessor): Unit = withContext(Dispatchers.EDT) {
        writeCommandAction(rootProject, "Replacing Accessor Body") {
            logger.debug { "Replacing accessor body: ${accessor.name} in ${accessor.containingFile.virtualFile.path}" }
            when {
                accessor.hasBlockBody() -> accessor.bodyBlockExpression!!.replace(psiFactory.createBlock(BLOCKLESS_TEXT))
                accessor.hasBody() -> accessor.bodyExpression!!.replace(psiFactory.createExpression(BLOCKLESS_TEXT))
            }
        }
    }

    suspend fun replaceBody(element: PsiElement): Unit = when (element) {
        is KtClassInitializer -> replaceBody(element)
        is KtNamedFunction -> replaceBody(element)
        is KtLambdaExpression -> replaceBody(element)
        is KtPropertyAccessor -> replaceBody(element)
        else -> error("Invalid PSI element type: ${element::class.simpleName}. Expected one of: KtClassInitializer, KtNamedFunction, KtLambdaExpression, KtPropertyAccessor")
    }

    suspend fun findAllPsiWithBodyItems(): List<PsiWithBodyDDItem> {
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
            )
        }
        logger.debug {
            "Found ${kotlinFiles.size} kotlin files"
        }
        if (kotlinFiles.isEmpty()) {
            logger.warn { "Found 0 kotlin files!" }
            logger.trace {
                val fileTypes = getAllFileTypesInProject(rootProject)
                val asString = fileTypes
                    .toList()
                    .map { (type, files) -> "${type.name}: ${files.map {it.toNioPath().relativeTo(rootProject.guessProjectDir()!!.toNioPath()) }}"}
                "However, there are fileTypes and its files:\n$asString"}
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


            smartReadAction(rootProject) {
                PsiWithBodyDDItem.PSI_ALL_JAVA_CLASSES.flatMap { clazz ->
                    PsiTreeUtil.collectElementsOfType(ktFile, clazz)
                        .filter { PsiWithBodyDDItem.hasBodyIfAvailable(it) != false }
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

   private fun getAllFileTypesInProject(project: Project): Map<FileType, List<VirtualFile>> = buildMap {

        FileBasedIndex.getInstance().processAllKeys(
            FileTypeIndex.NAME, { fileType ->
                val filesOfType = FileBasedIndex.getInstance().getContainingFiles(
                    FileTypeIndex.NAME, fileType, project.projectScope()
                )
                if (filesOfType.isNotEmpty()) {
                    put(fileType, filesOfType.toList())
                }
                true
            }, project
        )
    }

    companion object {
        private const val BLOCKLESS_TEXT = "TODO(\"Removed by DD\")"
    }
}
