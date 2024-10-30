package org.plan.research.minimization.plugin.psi

import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem
import org.plan.research.minimization.plugin.services.RootsManagerService

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.command.writeCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.psi.util.PsiTreeUtil
import mu.KotlinLogging
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.*

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private typealias ClassKtExpression = Class<out KtExpression>

/**
 * Service that provides functions to modify the bodies of various Kotlin elements within a project.
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
        val rootManager = rootProject.service<RootsManagerService>()
        val roots = smartReadAction(rootProject) {
            rootManager
                .findPossibleRoots()
                .takeIf { it.isNotEmpty() }
                ?: listOf(rootProject.guessProjectDir()!!)
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

    companion object {
        private const val BLOCKLESS_TEXT = "TODO(\"Removed by DD\")"
    }
}
